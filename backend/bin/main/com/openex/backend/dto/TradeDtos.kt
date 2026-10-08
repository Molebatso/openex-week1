package com.openex.backend.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class TradeResponse(
    val id: UUID,
    val symbol: String,
    val buyOrderId: UUID,
    val sellOrderId: UUID,
    val price: BigDecimal,
    val quantity: BigDecimal,
    val total: BigDecimal,
    val executedAt: Instant,
)
