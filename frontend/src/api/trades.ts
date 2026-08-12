import apiClient from './client';
import type { TradeResponse } from '../types';

export const tradesApi = {
  myTrades: () =>
    apiClient.get<TradeResponse[]>('/trades').then((r) => r.data),

  recentTrades: (symbol: string) =>
    apiClient.get<TradeResponse[]>(`/market/recent-trades?symbol=${encodeURIComponent(symbol)}`).then((r) => r.data),
};
