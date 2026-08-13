import apiClient from './client';
import type { WalletResponse } from '../types';

export const walletApi = {
  list: () =>
    apiClient.get<WalletResponse[]>('/wallet').then((r) => r.data),
};
