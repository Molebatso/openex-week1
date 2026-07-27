package com.openex.backend.repository

import com.openex.backend.entity.LedgerEntry
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LedgerEntryRepository : JpaRepository<LedgerEntry, UUID> {
    fun findAllByTradeId(tradeId: UUID): List<LedgerEntry>
    fun findAllByWalletId(walletId: UUID): List<LedgerEntry>
}
