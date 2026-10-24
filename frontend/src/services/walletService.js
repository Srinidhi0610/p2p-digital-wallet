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

  /**
   * Deposits demo funds from the system treasury faucet into the user's wallet.
   * Safe development-only demo feature that updates authoritative balance and writes double-entry ledger entries.
   */
  async depositDemoFunds(walletId, amount, idempotencyKey) {
    const payload = {
      amount: typeof amount === 'string' ? parseFloat(amount) : amount,
    }
    if (idempotencyKey) {
      payload.idempotencyKey = idempotencyKey
    }
    return await apiRequest(`/wallets/${walletId}/deposit`, {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },
}
