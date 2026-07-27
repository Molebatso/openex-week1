package com.openex.backend.repository

import com.openex.backend.entity.Trade
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface TradeRepository : JpaRepository<Trade, UUID> {
    @Query("""
        SELECT t FROM Trade t
        WHERE t.buyOrder.user.id = :userId OR t.sellOrder.user.id = :userId
        ORDER BY t.executedAt DESC
    """)
    fun findAllByUserId(userId: UUID): List<Trade>
}
