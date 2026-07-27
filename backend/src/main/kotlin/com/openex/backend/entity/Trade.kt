package com.openex.backend.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Immutable record of a matched execution between a buy and sell order. */
@Entity
@Table(name = "trades")
data class Trade(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buy_order_id", nullable = false)
    val buyOrder: Order,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sell_order_id", nullable = false)
    val sellOrder: Order,

    @Column(nullable = false, length = 20)
    val symbol: String,

    @Column(nullable = false, precision = 24, scale = 8)
    val price: BigDecimal,

    @Column(nullable = false, precision = 24, scale = 8)
    val quantity: BigDecimal,

    @Column(nullable = false, updatable = false)
    val executedAt: Instant = Instant.now(),
)
