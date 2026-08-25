package com.openex.backend.service

import com.openex.backend.dto.TradeResponse
import com.openex.backend.repository.TradeRepository
import com.openex.backend.repository.UserRepository
import com.openex.backend.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TradeService(
    private val tradeRepository: TradeRepository,
    private val userRepository: UserRepository,
) {
    /**
     * Return all trades in which the user participated (as buyer or seller),
     * sorted newest first.
     */
    @Transactional(readOnly = true)
    fun getTradesForUser(userId: UUID): List<TradeResponse> {
        userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found") }

        return tradeRepository.findAllByUserId(userId).map { trade ->
            TradeResponse(
                id = trade.id!!,
                symbol = trade.symbol,
                buyOrderId = trade.buyOrder.id!!,
                sellOrderId = trade.sellOrder.id!!,
                price = trade.price,
                quantity = trade.quantity,
                total = trade.price.multiply(trade.quantity),
                executedAt = trade.executedAt,
            )
        }
    }
}
