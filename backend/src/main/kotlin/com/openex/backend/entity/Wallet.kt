package com.openex.backend.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "wallets",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_wallet_user_currency",
            columnNames = ["user_id", "currency"]
        )
    ]
)
class Wallet(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

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