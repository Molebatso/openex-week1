package com.openex.backend.controller

import com.openex.backend.dto.OrderBookResponse
import com.openex.backend.matching.MatchingEngineService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/orderbook")
@Tag(name = "Order Book", description = "Live order book snapshots")
@SecurityRequirement(name = "bearerAuth")
class OrderBookController(
    private val matchingEngineService: MatchingEngineService,
) {

    /**
     * GET /api/orderbook?symbol=BTC/USD&depth=20
     *
     * Returns a snapshot of the current in-memory order book.
     * Requires authentication.
     *
     * @param symbol Trading pair (e.g. "BTC/USD")
     * @param depth  Max number of price levels per side (default 20, max 50)
     */
    @GetMapping
    @Operation(summary = "Get live order book snapshot for a symbol")
    fun getOrderBook(
        @RequestParam symbol: String,
        @RequestParam(defaultValue = "20") depth: Int,
    ): ResponseEntity<OrderBookResponse> {
        val safeDepth = depth.coerceIn(1, 50)
        return ResponseEntity.ok(
            matchingEngineService.getOrderBookSnapshot(symbol.uppercase(), safeDepth)
        )
    }
}
