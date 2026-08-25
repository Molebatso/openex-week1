package com.openex.backend.controller

import com.openex.backend.dto.TradeResponse
import com.openex.backend.security.OpenExUserDetails
import com.openex.backend.service.TradeService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/trades")
class TradeController(private val tradeService: TradeService) {

    /** GET /api/trades — list all trades the authenticated user participated in */
    @GetMapping
    fun getTrades(
        @AuthenticationPrincipal principal: OpenExUserDetails,
    ): ResponseEntity<List<TradeResponse>> =
        ResponseEntity.ok(tradeService.getTradesForUser(principal.userId))
}
