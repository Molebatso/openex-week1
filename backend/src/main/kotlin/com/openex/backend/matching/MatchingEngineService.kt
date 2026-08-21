package com.openex.backend.matching

import com.openex.backend.dto.OrderBookResponse
import com.openex.backend.dto.PriceLevel
import com.openex.backend.entity.Order
import com.openex.backend.entity.OrderSide
import com.openex.backend.entity.OrderStatus
import com.openex.backend.entity.OrderType
import com.openex.backend.entity.Trade
import com.openex.backend.exception.InvalidOrderException
import com.openex.backend.ledger.LedgerService
import com.openex.backend.repository.OrderRepository
import com.openex.backend.repository.TradeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Central matching engine.
 *
 * Each trading symbol gets its own [OrderBook].
 * Matching runs in-memory; trade + ledger persistence happens inside
 * a single [@Transactional] boundary so failures roll back atomically.
 *
 * Symbol format: "BASE/QUOTE" e.g. "BTC/USD"
 */
@Service
class MatchingEngineService(
    private val orderRepository: OrderRepository,
    private val tradeRepository: TradeRepository,
    private val ledgerService: LedgerService,
) {
    private val log = LoggerFactory.getLogger(MatchingEngineService::class.java)

    /** One order book per symbol */
    private val orderBooks = ConcurrentHashMap<String, OrderBook>()

    private fun bookFor(symbol: String): OrderBook =
        orderBooks.getOrPut(symbol) { OrderBook(symbol) }

    /**
     * Submit an order to the matching engine.
     *
     * 1. Validate the order
     * 2. Attempt to match against the opposing side
     * 3. Persist all resulting trades + ledger entries (in one transaction)
     * 4. If the order is not fully filled and it is a LIMIT order, add it to the book
     * 5. Update and persist final order statuses
     *
     * @return list of trades produced (may be empty)
     */
    @Transactional
    fun submit(order: Order): List<Trade> {
        validateOrder(order)

        val (baseCurrency, quoteCurrency) = parseSymbol(order.symbol)
        val book = bookFor(order.symbol)

        val matchResults = book.match(order)
        val trades = mutableListOf<Trade>()

        for (result in matchResults) {
            val trade = tradeRepository.save(
                Trade(
                    buyOrder = result.buyOrder,
                    sellOrder = result.sellOrder,
                    symbol = order.symbol,
                    price = result.price,
                    quantity = result.quantity,
                )
            )

            // Settle through double-entry ledger
            ledgerService.settleTrade(trade, baseCurrency, quoteCurrency)
            trades += trade

            // Persist resting order status
            val restingOrder = if (order.side == OrderSide.BUY) result.sellOrder else result.buyOrder
            updateOrderStatus(restingOrder)
            orderRepository.save(restingOrder)

            log.info(
                "TRADE executed — {} {} @ {} [buy:{} sell:{}]",
                result.quantity, baseCurrency, result.price,
                result.buyOrder.id, result.sellOrder.id,
            )
        }

        // Update incoming order status
        updateOrderStatus(order)

        // Resting LIMIT orders that are not fully filled go onto the book
        if (order.type == OrderType.LIMIT && order.status != OrderStatus.FILLED) {
            book.add(order)
        }

        orderRepository.save(order)
        return trades
    }

    /**
     * Cancel an open order.
     * Removes it from the in-memory book and marks it CANCELLED in the DB.
     */
    @Transactional
    fun cancel(orderId: UUID): Order {
        val order = orderRepository.findById(orderId)
            .orElseThrow { com.openex.backend.exception.ResourceNotFoundException("Order $orderId not found") }

        if (order.status == OrderStatus.FILLED || order.status == OrderStatus.CANCELLED) {
            throw InvalidOrderException("Cannot cancel order with status ${order.status}")
        }

        bookFor(order.symbol).remove(order)
        order.status = OrderStatus.CANCELLED
        return orderRepository.save(order).also {
            log.info("Order {} cancelled", orderId)
        }
    }

    /**
     * Return a snapshot of the current order book for a symbol.
     * Safe to call from outside a transaction — reads from the in-memory book.
     *
     * @param depth maximum number of price levels to return per side (default 20)
     */
    fun getOrderBookSnapshot(symbol: String, depth: Int = 20): OrderBookResponse {
        val book = bookFor(symbol)
        val bids = book.bidDepth().entries.take(depth).map { (p, q) -> PriceLevel(p, q) }
        val asks = book.askDepth().entries.take(depth).map { (p, q) -> PriceLevel(p, q) }
        return OrderBookResponse(symbol = symbol, bids = bids, asks = asks)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun validateOrder(order: Order) {
        if (order.type == OrderType.LIMIT && order.price == null) {
            throw InvalidOrderException("LIMIT order requires a price")
        }
        if (order.quantity <= java.math.BigDecimal.ZERO) {
            throw InvalidOrderException("Order quantity must be positive")
        }
    }

    private fun updateOrderStatus(order: Order) {
        order.status = when {
            order.filledQuantity >= order.quantity           -> OrderStatus.FILLED
            order.filledQuantity > java.math.BigDecimal.ZERO -> OrderStatus.PARTIAL
            else                                             -> OrderStatus.OPEN
        }
    }

    private fun parseSymbol(symbol: String): Pair<String, String> {
        val parts = symbol.split("/")
        if (parts.size != 2) throw InvalidOrderException("Invalid symbol format: $symbol (expected BASE/QUOTE)")
        return parts[0] to parts[1]
    }

    /** Expose raw bid/ask depth maps for WebSocket publishing without DTO conversion. */
    fun getBidDepth(symbol: String) = bookFor(symbol).bidDepth()
    fun getAskDepth(symbol: String) = bookFor(symbol).askDepth()
}
