package com.openex.backend.dto

import java.math.BigDecimal
import java.util.UUID
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank

data class WalletResponse(
    val id: UUID,
    val currency: String,
    val balance: BigDecimal,
)

data class DepositRequest(
    @field:NotBlank(message = "Currency is required")
    val currency: String,
    @field:DecimalMin(value = "0.00000001", message = "Amount must be greater than zero")
    val amount: BigDecimal,
)
