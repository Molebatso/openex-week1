package com.openex.backend.dto

import java.math.BigDecimal
import java.time.Instant

/** A single price level in the order book (price + aggregated quantity). */
data class PriceLevel(
    val price: BigDecimal,
    val quantity: BigDecimal,
)

/** Snapshot of the live order book for a symbol. */
data class OrderBookResponse(
    val symbol: String,
    val bids: List<PriceLevel>,
    val asks: List<PriceLevel>,
    val timestamp: Instant = Instant.now(),
)

/** Latest traded price for a symbol. */
data class MarketPriceResponse(
    val symbol: String,
    val price: BigDecimal?,
    val timestamp: Instant = Instant.now(),
)

/** 24-hour rolling statistics for a symbol. */
data class MarketStatsResponse(
    val symbol: String,
    val latestPrice: BigDecimal?,
    val high24h: BigDecimal?,
    val low24h: BigDecimal?,
    val volume24h: BigDecimal,
    val priceChange24h: BigDecimal?,
    val priceChangePct24h: BigDecimal?,
    val timestamp: Instant = Instant.now(),
)

// ── WebSocket event envelopes ──────────────────────────────────────────────

data class WsOrderBookEvent(
    val type: String = "ORDER_BOOK_UPDATE",
    val symbol: String,
    val bids: List<PriceLevel>,
    val asks: List<PriceLevel>,
    val timestamp: Instant = Instant.now(),
)

data class WsTradeEvent(
    val type: String = "TRADE",
    val id: java.util.UUID,
    val symbol: String,
    val price: BigDecimal,
    val quantity: BigDecimal,
    val total: BigDecimal,
    val executedAt: Instant,
)

data class WsMarketUpdateEvent(
    val type: String = "MARKET_UPDATE",
    val symbol: String,
    val latestPrice: BigDecimal?,
    val high24h: BigDecimal?,
    val low24h: BigDecimal?,
    val volume24h: BigDecimal,
    val timestamp: Instant = Instant.now(),
)
