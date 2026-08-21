package com.openex.backend.repository

import com.openex.backend.entity.Trade
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface TradeRepository : JpaRepository<Trade, UUID> {

    /** All trades where the user was buyer or seller, newest first. */
    @Query("""
        SELECT t FROM Trade t
        WHERE t.buyOrder.user.id = :userId OR t.sellOrder.user.id = :userId
        ORDER BY t.executedAt DESC
    """)
    fun findAllByUserId(userId: UUID): List<Trade>

    /** Most recent trades for a symbol (public market data). */
    fun findTop50BySymbolOrderByExecutedAtDesc(symbol: String): List<Trade>

    /** All trades for a symbol executed after a given instant (used for 24 h stats). */
    fun findAllBySymbolAndExecutedAtAfter(symbol: String, since: Instant): List<Trade>

    /** Latest single trade for a symbol (for last-traded price). */
    fun findFirstBySymbolOrderByExecutedAtDesc(symbol: String): Trade?
}
