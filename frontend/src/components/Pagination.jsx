import { ChevronLeft, ChevronRight } from 'lucide-react'

export function Pagination({ currentPage, totalPages, onPageChange, isLoading }) {
  if (totalPages <= 1) return null

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '1.25rem 0 0.5rem',
        borderTop: '1px solid var(--slate-border)',
        marginTop: '1rem',
      }}
    >
      <div style={{ fontSize: '0.8rem', fontFamily: 'var(--font-mono)', color: 'var(--muted)' }}>
        PAGE <span style={{ color: 'var(--lime)', fontWeight: 700 }}>{currentPage + 1}</span> /{' '}
        <span>{totalPages}</span>
      </div>

      <div style={{ display: 'flex', gap: '0.5rem' }}>
        <button
          className="btn btn-secondary btn-sm"
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage <= 0 || isLoading}
          title="Previous page"
        >
          <ChevronLeft size={15} />
          <span>Prev</span>
        </button>

        <button
          className="btn btn-secondary btn-sm"
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage >= totalPages - 1 || isLoading}
          title="Next page"
        >
          <span>Next</span>
          <ChevronRight size={15} />
        </button>
      </div>
    </div>
  )
}
