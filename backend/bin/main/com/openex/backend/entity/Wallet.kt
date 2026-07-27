package com.openex.backend.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "wallets",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "currency"])]
)
data class Wallet(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, length = 10)
    val currency: String,

    @Column(nullable = false, precision = 24, scale = 8)
    var balance: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)
