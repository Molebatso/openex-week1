package com.openex.backend.service

import com.openex.backend.dto.MarketPriceResponse
import com.openex.backend.dto.MarketStatsResponse
import com.openex.backend.dto.WsMarketUpdateEvent
import com.openex.backend.repository.TradeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks real-time market data — latest traded prices and 24-hour statistics.
 *
 * Latest prices are held in-memory (updated every time a trade executes).
 * Stats are computed on demand from the TradeRepository using a 24 h window.
 */
@Service
class MarketDataService(
    private val tradeRepository: TradeRepository,
) {
    private val log = LoggerFactory.getLogger(MarketDataService::class.java)

    /** In-memory cache of the last execution price per symbol. */
    private val latestPrices = ConcurrentHashMap<String, BigDecimal>()

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Called by [OrderService] after each trade execution to keep the
     * in-memory price cache current.
     */
    fun updateLatestPrice(symbol: String, price: BigDecimal) {
        latestPrices[symbol] = price
        log.debug("Market price updated: {} = {}", symbol, price)
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /** Latest traded price for a symbol (null if no trades yet). */
    @Transactional(readOnly = true)
    fun getLatestPrice(symbol: String): MarketPriceResponse {
        // Prefer in-memory cache; fall back to DB on cold start
        val price = latestPrices[symbol]
            ?: tradeRepository.findFirstBySymbolOrderByExecutedAtDesc(symbol)?.price
        return MarketPriceResponse(symbol = symbol, price = price)
    }

    /** 24-hour rolling statistics for a symbol. */
    @Transactional(readOnly = true)
    fun getStats(symbol: String): MarketStatsResponse {
        val since = Instant.now().minus(24, ChronoUnit.HOURS)
        val trades = tradeRepository.findAllBySymbolAndExecutedAtAfter(symbol, since)

        val latestPrice = latestPrices[symbol]
            ?: tradeRepository.findFirstBySymbolOrderByExecutedAtDesc(symbol)?.price

        if (trades.isEmpty()) {
            return MarketStatsResponse(
                symbol = symbol,
                latestPrice = latestPrice,
                high24h = null,
                low24h = null,
                volume24h = BigDecimal.ZERO,
                priceChange24h = null,
                priceChangePct24h = null,
            )
        }

        val high = trades.maxOf { it.price }
        val low = trades.minOf { it.price }
        val volume = trades.sumOf { it.quantity }

        // Price change: latest vs. oldest trade in the 24 h window
        val oldest = trades.minByOrNull { it.executedAt }
        val priceChange = if (latestPrice != null && oldest != null)
            latestPrice.subtract(oldest.price) else null
        val pctChange = if (priceChange != null && oldest != null && oldest.price > BigDecimal.ZERO)
            priceChange.divide(oldest.price, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
        else null

        return MarketStatsResponse(
            symbol = symbol,
            latestPrice = latestPrice,
            high24h = high,
            low24h = low,
            volume24h = volume,
            priceChange24h = priceChange,
            priceChangePct24h = pctChange,
        )
    }

    /** Build a [WsMarketUpdateEvent] for broadcasting to WebSocket clients. */
    fun buildMarketUpdateEvent(symbol: String): WsMarketUpdateEvent {
        val stats = getStats(symbol)
        return WsMarketUpdateEvent(
            symbol = symbol,
            latestPrice = stats.latestPrice,
            high24h = stats.high24h,
            low24h = stats.low24h,
            volume24h = stats.volume24h,
        )
    }

    private fun <T> Iterable<T>.sumOf(selector: (T) -> BigDecimal): BigDecimal =
        fold(BigDecimal.ZERO) { acc, item -> acc + selector(item) }
}
