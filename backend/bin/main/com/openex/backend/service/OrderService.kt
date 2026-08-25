package com.openex.backend.service

import com.openex.backend.dto.OrderResponse
import com.openex.backend.dto.OrderRequest
import com.openex.backend.entity.Order
import com.openex.backend.entity.OrderType
import com.openex.backend.exception.InvalidOrderException
import com.openex.backend.exception.ResourceNotFoundException
import com.openex.backend.exception.UnauthorizedOperationException
import com.openex.backend.matching.MatchingEngineService
import com.openex.backend.repository.OrderRepository
import com.openex.backend.repository.UserRepository
import com.openex.backend.websocket.TradingEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val matchingEngineService: MatchingEngineService,
    private val tradingEventPublisher: TradingEventPublisher,
    private val marketDataService: MarketDataService,
) {
    private val log = LoggerFactory.getLogger(OrderService::class.java)

    /**
     * Place a new order.
     *
     * If an idempotency key is supplied and was already used, return the
     * cached order response instead of creating a duplicate.
     *
     * After a successful match, broadcasts:
     *  - Each executed trade to /topic/trades/{symbol}
     *  - A refreshed order-book snapshot to /topic/orderbook/{symbol}
     *  - Updated market stats to /topic/market/{symbol}
     */
    @Transactional
    fun placeOrder(request: OrderRequest, userId: UUID): OrderResponse {
        // ── Idempotency check ─────────────────────────────────────────────────
        if (request.idempotencyKey != null) {
            val existing = orderRepository.findByIdempotencyKey(request.idempotencyKey)
            if (existing != null) {
                log.info("Idempotent order response returned for key {}", request.idempotencyKey)
                return existing.toResponse()
            }
        }

        if (request.type == OrderType.LIMIT && request.price == null) {
            throw InvalidOrderException("LIMIT orders require a price")
        }

        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found") }

        val order = orderRepository.save(
            Order(
                user = user,
                symbol = request.symbol.uppercase(),
                side = request.side,
                type = request.type,
                price = request.price,
                quantity = request.quantity,
                idempotencyKey = request.idempotencyKey,
            )
        )

        // Submit to matching engine — may produce trades immediately
        val trades = matchingEngineService.submit(order)

        // ── Post-match WebSocket broadcasts (outside the DB transaction) ──────
        if (trades.isNotEmpty()) {
            val symbol = order.symbol
            trades.forEach { tradingEventPublisher.publishTrade(it) }
            trades.lastOrNull()?.let { marketDataService.updateLatestPrice(symbol, it.price) }

            tradingEventPublisher.publishOrderBook(
                symbol = symbol,
                bids = matchingEngineService.getBidDepth(symbol),
                asks = matchingEngineService.getAskDepth(symbol),
            )
            tradingEventPublisher.publishMarketUpdate(
                marketDataService.buildMarketUpdateEvent(symbol)
            )
        }

        log.info("Order placed: {} {} {} @ {} by {}", order.type, order.side, order.quantity, order.price, user.username)
        return order.toResponse()
    }

    /** Return all orders for a user, newest first. */
    @Transactional(readOnly = true)
    fun getOrders(userId: UUID): List<OrderResponse> {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found") }
        return orderRepository.findAllByUserOrderByCreatedAtDesc(user).map { it.toResponse() }
    }

    /** Return a single order by id; only the owner may view it. */
    @Transactional(readOnly = true)
    fun getOrder(orderId: UUID, userId: UUID): OrderResponse {
        val order = orderRepository.findById(orderId)
            .orElseThrow { ResourceNotFoundException("Order $orderId not found") }
        if (order.user.id != userId) {
            throw UnauthorizedOperationException("You do not own order $orderId")
        }
        return order.toResponse()
    }

    /** Cancel an open order. Only the order's owner may cancel it. */
    @Transactional
    fun cancelOrder(orderId: UUID, userId: UUID): OrderResponse {
        val order = orderRepository.findById(orderId)
            .orElseThrow { ResourceNotFoundException("Order $orderId not found") }

        if (order.user.id != userId) {
            throw UnauthorizedOperationException("You do not own order $orderId")
        }

        val cancelled = matchingEngineService.cancel(orderId)

        // Broadcast updated order book after cancellation
        val symbol = cancelled.symbol
        tradingEventPublisher.publishOrderBook(
            symbol = symbol,
            bids = matchingEngineService.getBidDepth(symbol),
            asks = matchingEngineService.getAskDepth(symbol),
        )

        return cancelled.toResponse()
    }

    private fun Order.toResponse() = OrderResponse(
        id = id!!,
        symbol = symbol,
        side = side,
        type = type,
        price = price,
        quantity = quantity,
        filledQuantity = filledQuantity,
        remainingQuantity = remainingQuantity,
        status = status,
        createdAt = createdAt,
    )
}
