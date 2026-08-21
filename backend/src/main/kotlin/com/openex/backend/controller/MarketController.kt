package com.openex.backend.controller

import com.openex.backend.dto.MarketPriceResponse
import com.openex.backend.dto.MarketStatsResponse
import com.openex.backend.dto.TradeResponse
import com.openex.backend.repository.TradeRepository
import com.openex.backend.service.MarketDataService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/market")
@Tag(name = "Market", description = "Market prices, statistics, and public trade feed")
@SecurityRequirement(name = "bearerAuth")
class MarketController(
    private val marketDataService: MarketDataService,
    private val tradeRepository: TradeRepository,
) {

    /**
     * GET /api/market/latest-price?symbol=BTC/USD
     *
     * Returns the most recent execution price for the symbol.
     */
    @GetMapping("/latest-price")
    @Operation(summary = "Get the latest traded price for a symbol")
    fun getLatestPrice(
        @RequestParam symbol: String,
    ): ResponseEntity<MarketPriceResponse> =
        ResponseEntity.ok(marketDataService.getLatestPrice(symbol.uppercase()))

    /**
     * GET /api/market/stats?symbol=BTC/USD
     *
     * Returns 24-hour rolling statistics: high, low, volume, price change.
     */
    @GetMapping("/stats")
    @Operation(summary = "Get 24-hour market statistics for a symbol")
    fun getStats(
        @RequestParam symbol: String,
    ): ResponseEntity<MarketStatsResponse> =
        ResponseEntity.ok(marketDataService.getStats(symbol.uppercase()))

    /**
     * GET /api/market/recent-trades?symbol=BTC/USD
     *
     * Returns the 50 most recent public trades for a symbol.
     */
    @GetMapping("/recent-trades")
    @Operation(summary = "Get recent public trades for a symbol")
    fun getRecentTrades(
        @RequestParam symbol: String,
    ): ResponseEntity<List<TradeResponse>> {
        val trades = tradeRepository.findTop50BySymbolOrderByExecutedAtDesc(symbol.uppercase())
            .map { trade ->
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
        return ResponseEntity.ok(trades)
    }
}
