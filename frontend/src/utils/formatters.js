/**
 * Utility functions for formatting currencies, balances, and timestamps.
 */

export function formatCurrency(amount, currency = 'USD') {
  if (amount === null || amount === undefined || isNaN(amount)) {
    return '0.00'
  }

  const num = typeof amount === 'number' ? amount : parseFloat(amount)

  try {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency.toUpperCase(),
      minimumFractionDigits: 2,
      maximumFractionDigits: 4,
    }).format(num)
  } catch {
    // If currency code is not an official ISO code recognized by Intl
    return `${num.toFixed(4)} ${currency.toUpperCase()}`
  }
}

export function formatBalance(balance) {
  if (balance === null || balance === undefined || isNaN(balance)) {
    return '0.0000'
  }
  const num = typeof balance === 'number' ? balance : parseFloat(balance)
  return num.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 4,
  })
}

export function formatDateTime(isoString) {
  if (!isoString) return 'N/A'
  try {
    const date = new Date(isoString)
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    }).format(date)
  } catch {
    return isoString
  }
}

export function formatDateRelative(isoString) {
  if (!isoString) return 'N/A'
  try {
    const date = new Date(isoString)
    const now = new Date()
    const diffSeconds = Math.floor((now - date) / 1000)

    if (diffSeconds < 60) return 'Just now'
    if (diffSeconds < 3600) return `${Math.floor(diffSeconds / 60)}m ago`
    if (diffSeconds < 86400) return `${Math.floor(diffSeconds / 3600)}h ago`
    return formatDateTime(isoString)
  } catch {
    return isoString
  }
}

export function formatWalletId(id) {
  if (!id) return '#---'
  return `#W-${String(id).padStart(4, '0')}`
}
