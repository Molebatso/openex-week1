package com.openex.backend.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class EntryType { DEBIT, CREDIT }

/**
 * Immutable double-entry ledger record.
 * Every trade produces exactly four entries:
 *   - Buyer's quote-currency wallet: DEBIT (funds leave)
 *   - Buyer's base-currency wallet:  CREDIT (asset arrives)
 *   - Seller's base-currency wallet: DEBIT (asset leaves)
 *   - Seller's quote-currency wallet: CREDIT (funds arrive)
 */
@Entity
@Table(name = "ledger_entries")
data class LedgerEntry(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trade_id", nullable = false)
    val trade: Trade,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    val wallet: Wallet,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val entryType: EntryType,

    @Column(nullable = false, length = 10)
    val currency: String,

    /** Always positive; direction is expressed by entryType */
    @Column(nullable = false, precision = 24, scale = 8)
    val amount: BigDecimal,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)
