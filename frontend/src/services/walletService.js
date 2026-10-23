import { apiRequest } from './api'

export const walletService = {
  /**
   * Retrieves all wallets owned by the authenticated user.
   * Returns Array of WalletResponse: [{ id, userId, currency, balance, createdAt }]
   */
  async getWallets() {
    return await apiRequest('/wallets', {
      method: 'GET',
    })
  },

  /**
   * Retrieves a specific wallet by its numeric ID.
   * Ensures wallet belongs to authenticated user (or 403 Forbidden).
   */
  async getWalletById(walletId) {
    return await apiRequest(`/wallets/${walletId}`, {
      method: 'GET',
    })
  },

  /**
   * Creates a new currency wallet for the authenticated user.
   * Initial balance is always 0.0000.
   * Currency must be 3 letters (e.g. "USD", "EUR", "GBP").
   */
  async createWallet(currency) {
    return await apiRequest('/wallets', {
      method: 'POST',
      body: JSON.stringify({
        currency: currency.trim().toUpperCase(),
      }),
    })
  },
}
