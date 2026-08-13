import { useEffect, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type {
  WsTradeEvent,
  WsOrderBookEvent,
  WsMarketUpdateEvent,
} from '../types';

type WsEvent = WsTradeEvent | WsOrderBookEvent | WsMarketUpdateEvent;

interface UseWebSocketOptions {
  symbol: string;
  onTrade?: (event: WsTradeEvent) => void;
  onOrderBook?: (event: WsOrderBookEvent) => void;
  onMarketUpdate?: (event: WsMarketUpdateEvent) => void;
}

/**
 * Connects to the OpenEx STOMP WebSocket broker and subscribes to
 * market data topics for the given symbol.
 *
 * Automatically reconnects on disconnect.
 */
export function useWebSocket({
  symbol,
  onTrade,
  onOrderBook,
  onMarketUpdate,
}: UseWebSocketOptions) {
  const clientRef = useRef<Client | null>(null);
  const symbolRef = useRef(symbol);
  symbolRef.current = symbol;

  const handleMessage = useCallback(
    (raw: string, topic: string) => {
      try {
        const event: WsEvent = JSON.parse(raw);
        if (topic.startsWith('/topic/trades/')) {
          onTrade?.(event as WsTradeEvent);
        } else if (topic.startsWith('/topic/orderbook/')) {
          onOrderBook?.(event as WsOrderBookEvent);
        } else if (topic.startsWith('/topic/market/')) {
          onMarketUpdate?.(event as WsMarketUpdateEvent);
        }
      } catch {
        // ignore malformed messages
      }
    },
    [onTrade, onOrderBook, onMarketUpdate],
  );

  useEffect(() => {
    const wsUrl = `${window.location.protocol}//${window.location.host}/ws`;

    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 3000,
      onConnect: () => {
        const sym = symbolRef.current;
        client.subscribe(`/topic/trades/${sym}`, (msg) =>
          handleMessage(msg.body, `/topic/trades/${sym}`),
        );
        client.subscribe(`/topic/orderbook/${sym}`, (msg) =>
          handleMessage(msg.body, `/topic/orderbook/${sym}`),
        );
        client.subscribe(`/topic/market/${sym}`, (msg) =>
          handleMessage(msg.body, `/topic/market/${sym}`),
        );
      },
      onStompError: (frame) => {
        console.warn('STOMP error:', frame.headers['message']);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [symbol, handleMessage]);

  return clientRef.current;
}
