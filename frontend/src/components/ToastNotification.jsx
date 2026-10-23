import { useEffect } from 'react'
import { CheckCircle2, AlertCircle, Info, X } from 'lucide-react'

export function ToastNotification({ toast, onDismiss }) {
  useEffect(() => {
    if (!toast) return
    const timer = setTimeout(() => {
      onDismiss()
    }, 5000)
    return () => clearTimeout(timer)
  }, [toast, onDismiss])

  if (!toast) return null

  const isSuccess = toast.type === 'success'
  const isError = toast.type === 'error'

  return (
    <div
      style={{
        position: 'fixed',
        bottom: '1.5rem',
        right: '1.5rem',
        zIndex: 120,
        maxWidth: '420px',
        width: '100%',
      }}
    >
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '0.75rem',
          padding: '0.95rem 1.25rem',
          borderRadius: 'var(--radius-sm)',
          background: 'var(--slate-elevated)',
          border: '1px solid var(--slate-border-strong)',
          borderLeft: `4px solid ${
            isSuccess ? 'var(--lime)' : isError ? 'var(--coral)' : 'var(--muted)'
          }`,
          color: 'var(--ivory)',
          boxShadow: '0 10px 25px rgba(0, 0, 0, 0.6)',
        }}
      >
        {isSuccess && <CheckCircle2 size={18} color="var(--lime)" />}
        {isError && <AlertCircle size={18} color="var(--coral)" />}
        {!isSuccess && !isError && <Info size={18} color="var(--muted)" />}

        <div style={{ flex: 1 }}>
          {toast.title && <div style={{ fontWeight: 700, fontSize: '0.85rem' }}>{toast.title}</div>}
          <div style={{ fontSize: '0.82rem', color: 'var(--muted)' }}>{toast.message}</div>
        </div>

        <button
          onClick={onDismiss}
          style={{
            background: 'none',
            border: 'none',
            color: 'var(--muted)',
            cursor: 'pointer',
            padding: '4px',
          }}
        >
          <X size={15} />
        </button>
      </div>
    </div>
  )
}
