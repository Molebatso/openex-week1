package com.openex.backend.repository

import com.openex.backend.entity.Order
import com.openex.backend.entity.OrderStatus
import com.openex.backend.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OrderRepository : JpaRepository<Order, UUID> {
    fun findAllByUserOrderByCreatedAtDesc(user: User): List<Order>
    fun findByIdempotencyKey(idempotencyKey: UUID): Order?
    fun findAllBySymbolAndStatusIn(symbol: String, statuses: List<OrderStatus>): List<Order>
}
