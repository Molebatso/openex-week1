package com.openex.backend.dto

import com.openex.backend.entity.OrderSide
import com.openex.backend.entity.OrderStatus
import com.openex.backend.entity.OrderType
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class OrderRequest(
    @field:NotBlank(message = "Symbol is required (e.g. BTC/USD)")
    val symbol: String,

    @field:NotNull(message = "Side is required: BUY or SELL")
    val side: OrderSide,

    @field:NotNull(message = "Type is required: LIMIT or MARKET")
    val type: OrderType,

    /** Required for LIMIT orders; ignored for MARKET */
    @field:DecimalMin(value = "0.00000001", message = "Price must be greater than zero")
    val price: BigDecimal? = null,

    @field:NotNull(message = "Quantity is required")
    @field:DecimalMin(value = "0.00000001", message = "Quantity must be greater than zero")
    val quantity: BigDecimal,

    /** Optional client-supplied idempotency key */
    val idempotencyKey: UUID? = null,
)

data class OrderResponse(
    val id: UUID,
    val symbol: String,
    val side: OrderSide,
    val type: OrderType,
    val price: BigDecimal?,
    val quantity: BigDecimal,
    val filledQuantity: BigDecimal,
    val remainingQuantity: BigDecimal,
    val status: OrderStatus,
    val createdAt: Instant,
)
