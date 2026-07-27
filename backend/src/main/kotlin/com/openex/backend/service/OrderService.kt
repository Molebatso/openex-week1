package com.openex.backend.service

import com.openex.backend.dto.OrderRequest
import com.openex.backend.dto.OrderResponse
import com.openex.backend.entity.Order
import com.openex.backend.entity.OrderType
import com.openex.backend.exception.InvalidOrderException
import com.openex.backend.exception.ResourceNotFoundException
import com.openex.backend.exception.UnauthorizedOperationException
import com.openex.backend.matching.MatchingEngineService
import com.openex.backend.repository.OrderRepository
import com.openex.backend.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val matchingEngineService: MatchingEngineService,
) {
    private val log = LoggerFactory.getLogger(OrderService::class.java)

    /**
     * Place a new order.
     *
     * If an idempotency key is supplied and was already used, return the
     * cached order response instead of creating a duplicate.
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
        matchingEngineService.submit(order)

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

    /** Cancel an open order. Only the order's owner may cancel it. */
    @Transactional
    fun cancelOrder(orderId: UUID, userId: UUID): OrderResponse {
        val order = orderRepository.findById(orderId)
            .orElseThrow { ResourceNotFoundException("Order $orderId not found") }

        if (order.user.id != userId) {
            throw UnauthorizedOperationException("You do not own order $orderId")
        }

        return matchingEngineService.cancel(orderId).toResponse()
    }

    private fun Order.toResponse() = OrderResponse(
        id = id,
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
