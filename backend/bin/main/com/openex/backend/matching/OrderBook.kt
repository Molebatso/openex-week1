package com.openex.backend.matching

import com.openex.backend.entity.Order
import com.openex.backend.entity.OrderSide
import com.openex.backend.entity.OrderType
import java.math.BigDecimal
import java.util.TreeMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * In-memory order book for a single trading symbol.
 *
 * Bids  — sorted by price DESC, then by arrival time ASC (highest bid first).
 * Asks  — sorted by price ASC,  then by arrival time ASC (lowest ask first).
 *
 * Thread safety: all mutations are guarded by an internal [ReentrantLock].
 */
class OrderBook(val symbol: String) {

    private val lock = ReentrantLock()

    // price -> (orderId -> order), price-time priority
    private val bids: TreeMap<BigDecimal, LinkedHashMap<java.util.UUID, Order>> =
        TreeMap(reverseOrder())  // highest bid first

    private val asks: TreeMap<BigDecimal, LinkedHashMap<java.util.UUID, Order>> =
        TreeMap()                // lowest ask first

    // ── Public API ───────────────────────────────────────────────────────────

    /** Add an order to the book without attempting to match it. */
    fun add(order: Order) = lock.withLock {
        val book = if (order.side == OrderSide.BUY) bids else asks
        book.getOrPut(order.price!!) { LinkedHashMap() }[order.id] = order
    }

    /** Remove an order from the book (e.g. after fill or cancellation). */
    fun remove(order: Order) = lock.withLock {
        val book = if (order.side == OrderSide.BUY) bids else asks
        order.price?.let { price ->
            book[price]?.remove(order.id)
            if (book[price]?.isEmpty() == true) book.remove(price)
        }
    }

    /**
     * Match an incoming order against resting orders.
     *
     * Returns a list of [MatchResult] representing each partial or full fill.
     * The caller is responsible for persisting order + trade state.
     */
    fun match(incoming: Order): List<MatchResult> = lock.withLock {
        val results = mutableListOf<MatchResult>()
        var remaining = incoming.remainingQuantity

        when (incoming.side) {
            OrderSide.BUY  -> matchBuy(incoming, remaining, results)
            OrderSide.SELL -> matchSell(incoming, remaining, results)
        }

        results
    }

    /** Best bid price (or null if book is empty). */
    fun bestBid(): BigDecimal? = lock.withLock { bids.firstKey() }

    /** Best ask price (or null if book is empty). */
    fun bestAsk(): BigDecimal? = lock.withLock { asks.firstKey() }

    /** Snapshot of bids for market data (price -> total quantity). */
    fun bidDepth(): Map<BigDecimal, BigDecimal> = lock.withLock {
        bids.mapValues { (_, orders) -> orders.values.sumOf { it.remainingQuantity } }
    }

    /** Snapshot of asks for market data (price -> total quantity). */
    fun askDepth(): Map<BigDecimal, BigDecimal> = lock.withLock {
        asks.mapValues { (_, orders) -> orders.values.sumOf { it.remainingQuantity } }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun matchBuy(
        incoming: Order,
        startRemaining: BigDecimal,
        results: MutableList<MatchResult>,
    ) {
        var remaining = startRemaining
        val iter = asks.iterator()

        while (remaining > BigDecimal.ZERO && iter.hasNext()) {
            val (askPrice, orderMap) = iter.next()

            // For LIMIT orders check price; MARKET orders always match
            if (incoming.type == OrderType.LIMIT && incoming.price!! < askPrice) break

            val orderIter = orderMap.iterator()
            while (remaining > BigDecimal.ZERO && orderIter.hasNext()) {
                val (_, restingOrder) = orderIter.next()
                val fillQty = minOf(remaining, restingOrder.remainingQuantity)

                results += MatchResult(
                    buyOrder = incoming,
                    sellOrder = restingOrder,
                    price = askPrice,
                    quantity = fillQty,
                )

                remaining -= fillQty
                restingOrder.filledQuantity = restingOrder.filledQuantity.add(fillQty)
                incoming.filledQuantity = incoming.filledQuantity.add(fillQty)

                if (restingOrder.remainingQuantity <= BigDecimal.ZERO) {
                    orderIter.remove()
                }
            }

            if (orderMap.isEmpty()) iter.remove()
        }
    }

    private fun matchSell(
        incoming: Order,
        startRemaining: BigDecimal,
        results: MutableList<MatchResult>,
    ) {
        var remaining = startRemaining
        val iter = bids.iterator()

        while (remaining > BigDecimal.ZERO && iter.hasNext()) {
            val (bidPrice, orderMap) = iter.next()

            // For LIMIT orders check price; MARKET orders always match
            if (incoming.type == OrderType.LIMIT && incoming.price!! > bidPrice) break

            val orderIter = orderMap.iterator()
            while (remaining > BigDecimal.ZERO && orderIter.hasNext()) {
                val (_, restingOrder) = orderIter.next()
                val fillQty = minOf(remaining, restingOrder.remainingQuantity)

                results += MatchResult(
                    buyOrder = restingOrder,
                    sellOrder = incoming,
                    price = bidPrice,
                    quantity = fillQty,
                )

                remaining -= fillQty
                restingOrder.filledQuantity = restingOrder.filledQuantity.add(fillQty)
                incoming.filledQuantity = incoming.filledQuantity.add(fillQty)

                if (restingOrder.remainingQuantity <= BigDecimal.ZERO) {
                    orderIter.remove()
                }
            }

            if (orderMap.isEmpty()) iter.remove()
        }
    }
}

/**
 * Result of a single price-time match between a buy and sell order.
 */
data class MatchResult(
    val buyOrder: Order,
    val sellOrder: Order,
    val price: BigDecimal,
    val quantity: BigDecimal,
)
