package com.openex.backend.websocket

import com.openex.backend.dto.PriceLevel
import com.openex.backend.dto.WsMarketUpdateEvent
import com.openex.backend.dto.WsOrderBookEvent
import com.openex.backend.dto.WsTradeEvent
import com.openex.backend.entity.Trade
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.math.BigDecimal

/**
 * Publishes real-time trading events to WebSocket subscribers.
 *
 * Topics:
 *   /topic/trades/{symbol}      — each executed trade
 *   /topic/orderbook/{symbol}   — order-book snapshot after every change
 *   /topic/market/{symbol}      — market price & 24 h stats update
 */
@Service
class TradingEventPublisher(
    private val messagingTemplate: SimpMessagingTemplate,
) {
    private val log = LoggerFactory.getLogger(TradingEventPublisher::class.java)

    /** Broadcast a newly executed trade. */
    fun publishTrade(trade: Trade) {
        val event = WsTradeEvent(
            id = trade.id!!,
            symbol = trade.symbol,
            price = trade.price,
            quantity = trade.quantity,
            total = trade.price.multiply(trade.quantity),
            executedAt = trade.executedAt,
        )
        val destination = "/topic/trades/${trade.symbol}"
        messagingTemplate.convertAndSend(destination, event)
        log.debug("WS published TRADE to {} — {} @ {}", destination, trade.quantity, trade.price)
    }

    /** Broadcast a refreshed order-book snapshot for a symbol. */
    fun publishOrderBook(
        symbol: String,
        bids: Map<BigDecimal, BigDecimal>,
        asks: Map<BigDecimal, BigDecimal>,
        depth: Int = 20,
    ) {
        val event = WsOrderBookEvent(
            symbol = symbol,
            bids = bids.entries.take(depth).map { (p, q) -> PriceLevel(p, q) },
            asks = asks.entries.take(depth).map { (p, q) -> PriceLevel(p, q) },
        )
        val destination = "/topic/orderbook/$symbol"
        messagingTemplate.convertAndSend(destination, event)
        log.debug("WS published ORDER_BOOK_UPDATE to {}", destination)
    }

    /** Broadcast a market price + stats update. */
    fun publishMarketUpdate(event: WsMarketUpdateEvent) {
        val destination = "/topic/market/${event.symbol}"
        messagingTemplate.convertAndSend(destination, event)
        log.debug("WS published MARKET_UPDATE to {} — price={}", destination, event.latestPrice)
    }
}
