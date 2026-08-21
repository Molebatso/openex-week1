import apiClient from './client';
import type { DepositRequest, WalletResponse } from '../types';

export const walletApi = {
  list: () =>
    apiClient.get<WalletResponse[]>('/wallet').then((r) => r.data),
  deposit: (request: DepositRequest) =>
    apiClient.post<WalletResponse>('/wallet/deposit', request).then((r) => r.data),
};
