import { useState, useEffect, useCallback, useMemo } from 'react'
import { transferService } from '../services/transferService'
import { walletService } from '../services/walletService'
import { Pagination } from '../components/Pagination'
import { formatCurrency, formatDateTime } from '../utils/formatters'
import {
  History,
  ArrowUpRight,
  ArrowDownLeft,
  RefreshCw,
  AlertCircle,
  ShieldCheck,
  ExternalLink,
  X,
} from 'lucide-react'

export function Transactions() {
  const [transfers, setTransfers] = useState([])
  const [wallets, setWallets] = useState([])
  const [page, setPage] = useState(0)
  const size = 15
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [filterDirection, setFilterDirection] = useState('ALL') // 'ALL' | 'INCOMING' | 'OUTGOING'

  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)
  const [selectedTx, setSelectedTx] = useState(null)

  const loadData = useCallback(async (targetPage = 0) => {
    setIsLoading(true)
    setError(null)
    try {
      const [walletList, pageData] = await Promise.all([
        walletService.getWallets().catch(() => []),
        transferService.getTransfers({ page: targetPage, size }),
      ])

      setWallets(walletList || [])
      setTransfers(pageData?.content || [])
      setTotalPages(pageData?.totalPages || 0)
      setTotalElements(pageData?.totalElements || 0)
      setPage(pageData?.number || 0)
    } catch (err) {
      setError(err.message || 'Failed to retrieve double-entry transfer records.')
    } finally {
      setIsLoading(false)
    }
  }, [size])

  useEffect(() => {
    loadData(0)
  }, [loadData])

  function handlePageChange(newPage) {
    loadData(newPage)
  }

  const isUserWallet = useCallback((walletId) => {
    return wallets.some((w) => w.id === walletId)
  }, [wallets])

  // Filter transfers client-side based on direction
  const filteredTransfers = useMemo(() => {
    if (filterDirection === 'INCOMING') {
      return transfers.filter((t) => !isUserWallet(t.senderWalletId))
    }
    if (filterDirection === 'OUTGOING') {
      return transfers.filter((t) => isUserWallet(t.senderWalletId))
    }
    return transfers
  }, [transfers, filterDirection, isUserWallet])

  return (
    <div>
      {/* Header & Synchronization */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '1rem',
          marginBottom: '1.75rem',
        }}
      >
        <div>
          <div
            style={{
              fontFamily: 'var(--font-mono)',
              fontSize: '0.72rem',
              color: 'var(--lime)',
              letterSpacing: '0.08em',
              textTransform: 'uppercase',
              marginBottom: '0.35rem',
            }}
          >
            DOUBLE-ENTRY AUDIT TRAIL
          </div>
          <h1 style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--ivory)' }}>
            Transaction Ledger
          </h1>
          <p style={{ color: 'var(--muted)', fontSize: '0.88rem' }}>
            Immutable debit and credit transfer records ({totalElements} total entries)
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          {/* Direction Filter Buttons */}
          <div
            style={{
              display: 'flex',
              background: 'var(--slate)',
              border: '1px solid var(--slate-border)',
              borderRadius: 'var(--radius-sm)',
              padding: '2px',
            }}
          >
            <button
              onClick={() => setFilterDirection('ALL')}
              style={{
                fontFamily: 'var(--font-mono)',
                fontSize: '0.72rem',
                fontWeight: 600,
                padding: '0.3rem 0.65rem',
                borderRadius: 'var(--radius-xs)',
                background: filterDirection === 'ALL' ? 'var(--slate-elevated)' : 'transparent',
                color: filterDirection === 'ALL' ? 'var(--ivory)' : 'var(--muted)',
                border: 'none',
              }}
            >
              ALL
            </button>
            <button
              onClick={() => setFilterDirection('INCOMING')}
              style={{
                fontFamily: 'var(--font-mono)',
                fontSize: '0.72rem',
                fontWeight: 700,
                padding: '0.3rem 0.65rem',
                borderRadius: 'var(--radius-xs)',
                background: filterDirection === 'INCOMING' ? 'var(--lime-dim)' : 'transparent',
                color: filterDirection === 'INCOMING' ? 'var(--lime)' : 'var(--muted)',
                border: 'none',
              }}
            >
              + INCOMING
            </button>
            <button
              onClick={() => setFilterDirection('OUTGOING')}
              style={{
                fontFamily: 'var(--font-mono)',
                fontSize: '0.72rem',
                fontWeight: 700,
                padding: '0.3rem 0.65rem',
                borderRadius: 'var(--radius-xs)',
                background: filterDirection === 'OUTGOING' ? 'var(--coral-dim)' : 'transparent',
                color: filterDirection === 'OUTGOING' ? 'var(--coral)' : 'var(--muted)',
                border: 'none',
              }}
            >
              - OUTGOING
            </button>
          </div>

          <button
            onClick={() => loadData(page)}
            className="btn btn-secondary btn-sm"
            disabled={isLoading}
          >
            <RefreshCw size={13} className={isLoading ? 'spin' : ''} />
            <span>Sync</span>
          </button>
        </div>
      </div>

      {error && (
        <div className="alert alert-danger">
          <AlertCircle size={18} style={{ flexShrink: 0 }} />
          <span>{error}</span>
        </div>
      )}

      {/* Ledger Table Surface */}
      <div className="surface" style={{ padding: 0, overflow: 'hidden' }}>
        {isLoading ? (
          <div style={{ padding: '4rem', textAlign: 'center', color: 'var(--muted)' }}>
            <RefreshCw size={24} className="spin" style={{ margin: '0 auto 0.75rem' }} />
            <div>Streaming double-entry ledger records...</div>
          </div>
        ) : filteredTransfers.length === 0 ? (
          <div style={{ padding: '3.5rem', textAlign: 'center', color: 'var(--muted)' }}>
            <History size={36} style={{ margin: '0 auto 0.75rem', opacity: 0.3 }} />
            <h3 style={{ color: 'var(--ivory)', fontSize: '1.15rem' }}>No Transfers Found</h3>
            <p style={{ fontSize: '0.85rem', marginTop: '0.25rem' }}>
              {filterDirection === 'ALL'
                ? 'Your wallets have not participated in any ledger transfers yet.'
                : `No ${filterDirection.toLowerCase()} transactions match the active filter.`}
            </p>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="editorial-table">
              <thead>
                <tr>
                  <th>Direction</th>
                  <th>Amount</th>
                  <th>Source Wallet</th>
                  <th>Recipient Wallet</th>
                  <th>Settlement</th>
                  <th>Timestamp</th>
                  <th>Audit</th>
                </tr>
              </thead>
              <tbody>
                {filteredTransfers.map((tx) => {
                  const isSender = isUserWallet(tx.senderWalletId)
                  return (
                    <tr
                      key={tx.id}
                      style={{ cursor: 'pointer' }}
                      onClick={() => setSelectedTx(tx)}
                    >
                      <td>
                        {isSender ? (
                          <span className="flow-badge flow-outgoing">
                            <ArrowUpRight size={13} />
                            <span>Debit / Sent</span>
                          </span>
                        ) : (
                          <span className="flow-badge flow-incoming">
                            <ArrowDownLeft size={13} />
                            <span>Credit / Recv</span>
                          </span>
                        )}
                      </td>
                      <td>
                        <span className={isSender ? 'amount-outgoing' : 'amount-incoming'}>
                          {isSender ? '-' : '+'}
                          {formatCurrency(tx.amount, tx.currency)}
                        </span>
                      </td>
                      <td>
                        <span className="font-mono" style={{ fontSize: '0.82rem', color: 'var(--ivory)' }}>
                          ID #{tx.senderWalletId}
                          {isSender && (
                            <span style={{ color: 'var(--coral)', marginLeft: '4px', fontSize: '0.72rem' }}>
                              (You)
                            </span>
                          )}
                        </span>
                      </td>
                      <td>
                        <span className="font-mono" style={{ fontSize: '0.82rem', color: 'var(--ivory)' }}>
                          ID #{tx.recipientWalletId}
                          {!isSender && (
                            <span style={{ color: 'var(--lime)', marginLeft: '4px', fontSize: '0.72rem' }}>
                              (You)
                            </span>
                          )}
                        </span>
                      </td>
                      <td>
                        <span className="status-pill settled">
                          <ShieldCheck size={11} style={{ verticalAlign: 'middle', marginRight: '3px' }} />
                          {tx.status}
                        </span>
                      </td>
                      <td style={{ color: 'var(--muted)', fontSize: '0.8rem', fontFamily: 'var(--font-mono)' }}>
                        {formatDateTime(tx.createdAt)}
                      </td>
                      <td>
                        <button
                          className="btn btn-ghost btn-sm"
                          onClick={(e) => {
                            e.stopPropagation()
                            setSelectedTx(tx)
                          }}
                          style={{ padding: '0.2rem 0.4rem', color: 'var(--muted)' }}
                          title="Inspect cryptographic ledger details"
                        >
                          <ExternalLink size={13} />
                        </button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}

        <div style={{ padding: '0 1.25rem' }}>
          <Pagination
            currentPage={page}
            totalPages={totalPages}
            onPageChange={handlePageChange}
            isLoading={isLoading}
          />
        </div>
      </div>

      {/* Transaction Inspection Modal */}
      {selectedTx && (
        <div className="modal-backdrop" onClick={() => setSelectedTx(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
                <ShieldCheck size={20} color="var(--lime)" />
                <h3 style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--ivory)' }}>
                  Ledger Entry #{selectedTx.id}
                </h3>
              </div>
              <button
                onClick={() => setSelectedTx(null)}
                className="btn btn-ghost btn-sm"
                style={{ padding: '0.25rem' }}
              >
                <X size={18} />
              </button>
            </div>

            <div style={{ marginBottom: '1.5rem' }}>
              <div
                style={{
                  background: 'var(--navy)',
                  borderRadius: 'var(--radius-sm)',
                  padding: '1.25rem',
                  border: '1px solid var(--slate-border)',
                  marginBottom: '1rem',
                }}
              >
                <div style={{ textAlign: 'center', marginBottom: '1.25rem' }}>
                  <div style={{ fontSize: '0.68rem', fontFamily: 'var(--font-mono)', color: 'var(--muted)' }}>
                    COMMITTED AMOUNT
                  </div>
                  <div
                    style={{
                      fontFamily: 'var(--font-display)',
                      fontSize: '2.25rem',
                      fontWeight: 800,
                      color: isUserWallet(selectedTx.senderWalletId) ? 'var(--coral)' : 'var(--lime)',
                    }}
                  >
                    {isUserWallet(selectedTx.senderWalletId) ? '-' : '+'}
                    {formatCurrency(selectedTx.amount, selectedTx.currency)}
                  </div>
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.35rem 0', fontSize: '0.85rem' }}>
                  <span style={{ color: 'var(--muted)' }}>Source Wallet:</span>
                  <span className="font-mono">ID #{selectedTx.senderWalletId}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.35rem 0', fontSize: '0.85rem' }}>
                  <span style={{ color: 'var(--muted)' }}>Recipient Wallet:</span>
                  <span className="font-mono">ID #{selectedTx.recipientWalletId}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.35rem 0', fontSize: '0.85rem' }}>
                  <span style={{ color: 'var(--muted)' }}>Currency:</span>
                  <span className="font-mono">{selectedTx.currency}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.35rem 0', fontSize: '0.85rem' }}>
                  <span style={{ color: 'var(--muted)' }}>Settlement State:</span>
                  <span className="status-pill settled">
                    <ShieldCheck size={11} style={{ verticalAlign: 'middle', marginRight: '3px' }} />
                    {selectedTx.status}
                  </span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.35rem 0', fontSize: '0.85rem' }}>
                  <span style={{ color: 'var(--muted)' }}>Committed Timestamp:</span>
                  <span className="font-mono" style={{ fontSize: '0.8rem' }}>{formatDateTime(selectedTx.createdAt)}</span>
                </div>
                <div
                  style={{
                    borderTop: '1px solid var(--slate-border)',
                    paddingTop: '0.6rem',
                    marginTop: '0.6rem',
                  }}
                >
                  <span style={{ color: 'var(--muted)', display: 'block', fontSize: '0.68rem', fontFamily: 'var(--font-mono)' }}>
                    CLIENT IDEMPOTENCY KEY:
                  </span>
                  <span
                    className="font-mono"
                    style={{ fontSize: '0.72rem', color: 'var(--muted-dim)', wordBreak: 'break-all' }}
                  >
                    {selectedTx.idempotencyKey}
                  </span>
                </div>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setSelectedTx(null)}
              >
                Close Audit View
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
