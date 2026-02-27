import { Link } from 'react-router-dom'
import { usePayments } from '@/hooks/usePayments'
import AsyncState from '@/components/ui/AsyncState'
import { formatCurrency, formatDate, formatStatus } from '@/utils/formatters'
import type { Payment, Refund } from '@/types/payment'
import { useMemo, useState } from 'react'
import './RefundsList.css'

interface RefundWithPaymentInfo {
  refund: Refund
  payment: Payment
}

export default function RefundsList() {
  const { data, isLoading, isFetching, error, refetch } = usePayments()
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [search, setSearch] = useState('')

  const allRefunds = useMemo(() => {
    if (!data?.data) return []

    const refunds: RefundWithPaymentInfo[] = []
    data.data.forEach((payment: Payment) => {
      if (payment.refunds && payment.refunds.length > 0) {
        payment.refunds.forEach((refund) => {
          refunds.push({ refund, payment })
        })
      }
    })

    return refunds.sort(
      (a, b) => new Date(b.refund.createdAt).getTime() - new Date(a.refund.createdAt).getTime()
    )
  }, [data])

  const filteredRefunds = useMemo(() => {
    const query = search.trim().toLowerCase()

    return allRefunds.filter(({ refund, payment }) => {
      const statusMatch = !statusFilter || refund.status === statusFilter
      if (!statusMatch) return false

      if (!query) return true
      return (
        refund.id.toLowerCase().includes(query) ||
        payment.id.toLowerCase().includes(query) ||
        (payment.customer?.email || '').toLowerCase().includes(query)
      )
    })
  }, [allRefunds, search, statusFilter])

  const totalRefunded = allRefunds
    .filter((r) => r.refund.status === 'succeeded')
    .reduce((sum, r) => sum + r.refund.amount, 0)

  const successfulRefunds = allRefunds.filter((r) => r.refund.status === 'succeeded').length
  const failedRefunds = allRefunds.filter((r) => r.refund.status === 'failed').length
  const pendingRefunds = allRefunds.filter((r) => r.refund.status === 'pending').length

  if (isLoading) {
    return (
      <AsyncState
        kind="loading"
        title="Loading refunds"
        message="Gathering refund activity from your recent payments."
      />
    )
  }

  if (error) {
    return <AsyncState kind="error" title="Unable to load refunds" message={error.message} />
  }

  if (allRefunds.length === 0) {
    return (
      <AsyncState
        kind="empty"
        title="No refunds yet"
        message="Refunds will appear here after you process them from a payment detail page."
      />
    )
  }

  return (
    <div className="refunds-list">
      <div className="refunds-list__stats">
        <StatCard label="Total Refunded" value={formatCurrency(totalRefunded)} tone="danger" />
        <StatCard label="Successful" value={String(successfulRefunds)} tone="success" />
        <StatCard label="Failed" value={String(failedRefunds)} tone="danger" />
        <StatCard label="Pending" value={String(pendingRefunds)} tone="warning" />
      </div>

      <div className="refunds-list__panel">
        <div className="refunds-list__toolbar">
          <div>
            <p className="refunds-list__eyebrow">Filters</p>
            <h2>Refund records</h2>
          </div>
          <div className="refunds-list__toolbar-right">
            <div className="refunds-list__toolbar-meta">
              {filteredRefunds.length} shown
            </div>
            <span className="refunds-list__sync-state" role="status" aria-live="polite">
              {isFetching ? 'Refreshing...' : 'Auto-refresh on'}
            </span>
            <button type="button" className="refunds-list__refresh-btn" onClick={() => void refetch()}>
              Refresh
            </button>
          </div>
        </div>

        <div className="refunds-list__filters">
          <label className="refunds-list__field">
            <span>Search</span>
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Refund ID, payment ID, or email"
            />
          </label>

          <label className="refunds-list__field refunds-list__field--select">
            <span>Status</span>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="">All statuses</option>
              <option value="succeeded">Succeeded</option>
              <option value="pending">Pending</option>
              <option value="processing">Processing</option>
              <option value="failed">Failed</option>
              <option value="cancelled">Cancelled</option>
            </select>
          </label>
        </div>

        <div className="refunds-list__table-wrap">
          <table className="refunds-list__table">
            <thead>
              <tr>
                <th>Refund</th>
                <th>Payment</th>
                <th>Amount</th>
                <th>Status</th>
                <th>Reason</th>
                <th>Customer</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              {filteredRefunds.map(({ refund, payment }) => (
                <tr key={refund.id}>
                  <td>
                    <div className="refunds-list__id-cell">
                      <span className="refunds-list__id">{refund.id.substring(0, 8)}...</span>
                      <span className="refunds-list__subtle">{refund.id}</span>
                    </div>
                  </td>
                  <td>
                    <Link to={`/payments/${payment.id}`} className="refunds-list__payment-link">
                      {payment.id.substring(0, 8)}...
                    </Link>
                  </td>
                  <td className="refunds-list__amount-cell">
                    <div>{formatCurrency(refund.amount, refund.currency)}</div>
                    <span className="refunds-list__subtle">
                      of {formatCurrency(payment.amount, payment.currency)}
                    </span>
                  </td>
                  <td>
                    <span className={`refunds-list__status refunds-list__status--${refund.status}`}>
                      {formatStatus(refund.status)}
                    </span>
                  </td>
                  <td>{refund.reason || 'N/A'}</td>
                  <td>{payment.customer?.email || 'N/A'}</td>
                  <td>
                    <div>{formatDate(refund.createdAt)}</div>
                    {refund.completedAt && (
                      <span className="refunds-list__subtle refunds-list__subtle--success">
                        Completed {formatDate(refund.completedAt)}
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {filteredRefunds.length === 0 && (
          <AsyncState
            kind="empty"
            compact
            title="No refunds match your filters"
            message="Try changing the refund status filter or search query."
          />
        )}
      </div>
    </div>
  )
}

function StatCard({
  label,
  value,
  tone,
}: {
  label: string
  value: string
  tone: 'success' | 'warning' | 'danger'
}) {
  return (
    <div className={`refunds-list__stat-card refunds-list__stat-card--${tone}`}>
      <div className="refunds-list__stat-label">{label}</div>
      <div className="refunds-list__stat-value">{value}</div>
    </div>
  )
}
