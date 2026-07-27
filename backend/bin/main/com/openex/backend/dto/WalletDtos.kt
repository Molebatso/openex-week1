package com.openex.backend.dto

import java.math.BigDecimal
import java.util.UUID

data class WalletResponse(
    val id: UUID,
    val currency: String,
    val balance: BigDecimal,
)
