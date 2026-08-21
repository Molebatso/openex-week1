package com.openex.backend.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class OrderSide   { BUY, SELL }
enum class OrderType   { LIMIT, MARKET }
enum class OrderStatus { OPEN, PARTIAL, FILLED, CANCELLED }

@Entity
@Table(name = "orders")
data class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    /** Trading pair, e.g. "BTC/USD" */
    @Column(nullable = false, length = 20)
    val symbol: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val side: OrderSide,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val type: OrderType,

    /** Null for MARKET orders */
    @Column(precision = 24, scale = 8)
    val price: BigDecimal? = null,

    /** Original requested quantity */
    @Column(nullable = false, precision = 24, scale = 8)
    val quantity: BigDecimal,

    /** Quantity matched so far */
    @Column(nullable = false, precision = 24, scale = 8)
    var filledQuantity: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: OrderStatus = OrderStatus.OPEN,

    /** Client-supplied idempotency key (UUID) — unique per user per key */
    @Column(unique = true)
    val idempotencyKey: UUID? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
) {
    /** Remaining unfilled quantity */
    val remainingQuantity: BigDecimal
        get() = quantity - filledQuantity
}