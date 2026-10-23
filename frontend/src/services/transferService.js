import { apiRequest } from './api'
import { generateUUID } from '../utils/uuid'

export const transferService = {
  /**
   * Executes a peer-to-peer transfer between wallets using double-entry ledger settlement.
   * Automatically generates a UUIDv4 idempotency key if not explicitly supplied.
   *
   * @param {Object} transferData
   * @param {number|string} transferData.senderWalletId
   * @param {number|string} transferData.recipientWalletId
   * @param {number|string} transferData.amount
   * @param {string} transferData.currency
   * @param {string} [transferData.idempotencyKey]
   */
  async createTransfer({ senderWalletId, recipientWalletId, amount, currency, idempotencyKey }) {
    const key = idempotencyKey || generateUUID()

    // Ensure amount is formatted as number/valid decimal string
    const numAmount = typeof amount === 'string' ? parseFloat(amount) : amount

    const payload = {
      senderWalletId: Number(senderWalletId),
      recipientWalletId: Number(recipientWalletId),
      amount: numAmount,
      currency: currency.trim().toUpperCase(),
      idempotencyKey: key,
    }

    return await apiRequest('/transfers', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  /**
   * Retrieves paginated transfers where the authenticated user is either sender or recipient.
   *
   * @param {Object} [params]
   * @param {number} [params.page=0]
   * @param {number} [params.size=20]
   */
  async getTransfers({ page = 0, size = 20 } = {}) {
    const query = new URLSearchParams({
      page: String(page),
      size: String(size),
    }).toString()

    return await apiRequest(`/transfers?${query}`, {
      method: 'GET',
    })
  },

  /**
   * Retrieves single transfer details by ID.
   * Accessible only if user owns the sender or recipient wallet.
   */
  async getTransferById(transferId) {
    return await apiRequest(`/transfers/${transferId}`, {
      method: 'GET',
    })
  },
}
