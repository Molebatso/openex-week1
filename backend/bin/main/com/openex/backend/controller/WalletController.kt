package com.openex.backend.controller

import com.openex.backend.dto.DepositRequest
import com.openex.backend.dto.WalletResponse
import com.openex.backend.repository.UserRepository
import com.openex.backend.security.OpenExUserDetails
import com.openex.backend.service.WalletService
import com.openex.backend.exception.ResourceNotFoundException
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/wallet")
class WalletController(
    private val walletService: WalletService,
    private val userRepository: UserRepository,
) {
    /** GET /api/wallet — list all wallets for the authenticated user */
    @GetMapping
    fun getWallets(
        @AuthenticationPrincipal principal: OpenExUserDetails,
    ): ResponseEntity<List<WalletResponse>> {
        val user = userRepository.findById(principal.userId)
            .orElseThrow { ResourceNotFoundException("User not found") }
        return ResponseEntity.ok(walletService.getWallets(user))
    }

    /** POST /api/wallet/deposit — add simulated funds to one of the user's wallets. */
    @PostMapping("/deposit")
    fun deposit(
        @AuthenticationPrincipal principal: OpenExUserDetails,
        @Valid @RequestBody request: DepositRequest,
    ): ResponseEntity<WalletResponse> {
        val user = userRepository.findById(principal.userId)
            .orElseThrow { ResourceNotFoundException("User not found") }
        val wallet = walletService.credit(user, request.currency, request.amount)
        return ResponseEntity.ok(
            WalletResponse(wallet.id!!, wallet.currency, wallet.balance)
        )
    }
}
