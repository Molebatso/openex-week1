package com.openex.backend.config

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

/**
 * Configures STOMP over WebSocket.
 *
 * Clients connect to /ws (with SockJS fallback) and subscribe to:
 *   /topic/orderbook/{symbol}   — live order-book snapshots
 *   /topic/trades/{symbol}      — executed trade events
 *   /topic/market/{symbol}      — latest price + 24 h stats
 */
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        // Simple in-memory broker for /topic destinations
        registry.enableSimpleBroker("/topic")
        // Prefix for messages routed to @MessageMapping controllers
        registry.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry
            .addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS()
    }
}
