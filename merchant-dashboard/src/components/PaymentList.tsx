import { Link } from 'react-router-dom'
import { usePayments } from '@/hooks/usePayments'
import AsyncState from '@/components/ui/AsyncState'
import { formatCurrency, formatDate, formatStatus } from '@/utils/formatters'
import { useMemo, useState } from 'react'
import './PaymentList.css'

export default function PaymentList() {
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [search, setSearch] = useState('')
  const { data, isLoading, error } = usePayments(statusFilter || undefined)

  if (isLoading) {
    return (
      <AsyncState
        kind="loading"
        title="Loading payments"
        message="Fetching payment records and statuses."
      />
    )
  }

  if (error) {
    return (
      <AsyncState
        kind="error"
        title="Unable to load payments"
        message={error.message}
      />
    )
  }

  const payments = data?.data || []
  const searchTerm = search.trim().toLowerCase()

  const filteredPayments = useMemo(() => {
    if (!searchTerm) return payments
    return payments.filter((payment) => {
      const idMatch = payment.id.toLowerCase().includes(searchTerm)
      const emailMatch = payment.customer?.email?.toLowerCase().includes(searchTerm)
      return idMatch || emailMatch
    })
  }, [payments, searchTerm])

  const summary = {
    total: payments.length,
    captured: payments.filter((p) => p.status === 'captured').length,
    pending: payments.filter((p) => p.status === 'pending').length,
    attention: payments.filter((p) => p.status === 'failed' || p.status === 'declined').length,
  }

  return (
    <div className="payment-list">
      <div className="payment-list__toolbar-card">
        <div className="payment-list__toolbar-top">
          <div>
            <p className="payment-list__eyebrow">Filters</p>
            <h2>Browse payments</h2>
          </div>
          <Link to="/create-payment" className="payment-list__new-btn">
            New Payment
          </Link>
        </div>

        <div className="payment-list__summary">
          <SummaryChip label="Total" value={summary.total} tone="neutral" />
          <SummaryChip label="Captured" value={summary.captured} tone="success" />
          <SummaryChip label="Pending" value={summary.pending} tone="warning" />
          <SummaryChip label="Needs Attention" value={summary.attention} tone="danger" />
        </div>

        <div className="payment-list__filters">
          <label className="payment-list__field">
            <span>Search</span>
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Payment ID or customer email"
            />
          </label>

          <label className="payment-list__field payment-list__field--select">
            <span>Status</span>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="">All statuses</option>
              <option value="pending">Pending</option>
              <option value="authorized">Authorized</option>
              <option value="captured">Captured</option>
              <option value="partially_refunded">Partially Refunded</option>
              <option value="refunded">Refunded</option>
              <option value="failed">Failed</option>
              <option value="declined">Declined</option>
            </select>
          </label>
        </div>
      </div>

      <div className="payment-list__table-card">
        <div className="payment-list__table-header">
          <div>
            <h3>Payment records</h3>
            <p>{filteredPayments.length} shown{filteredPayments.length !== payments.length ? ` • ${payments.length} total` : ''}</p>
          </div>
        </div>

        {filteredPayments.length === 0 ? (
          <AsyncState
            kind="empty"
            compact
            title={payments.length === 0 ? 'No payments yet' : 'No payments match your filters'}
            message={
              payments.length === 0
                ? 'Create your first payment to populate this table.'
                : 'Try changing the status filter or search query.'
            }
          />
        ) : (
          <div className="payment-list__table-wrap">
            <table className="payment-list__table">
              <thead>
                <tr>
                  <th>Payment</th>
                  <th>Customer</th>
                  <th>Amount</th>
                  <th>Status</th>
                  <th>Created</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {filteredPayments.map((payment) => (
                  <tr key={payment.id}>
                    <td>
                      <div className="payment-list__primary-cell">
                        <Link to={`/payments/${payment.id}`} className="payment-list__id-link">
                          {payment.id.substring(0, 8)}...
                        </Link>
                        <span className="payment-list__subtle">{payment.id}</span>
                      </div>
                    </td>
                    <td>
                      <div className="payment-list__primary-cell">
                        <span>{payment.customer?.email || 'N/A'}</span>
                        {payment.customer?.name && (
                          <span className="payment-list__subtle">{payment.customer.name}</span>
                        )}
                      </div>
                    </td>
                    <td className="payment-list__amount">
                      {formatCurrency(payment.amount, payment.currency)}
                    </td>
                    <td>
                      <span className={`payment-list__status payment-list__status--${payment.status}`}>
                        {formatStatus(payment.status)}
                      </span>
                    </td>
                    <td>{formatDate(payment.createdAt)}</td>
                    <td>
                      <Link to={`/payments/${payment.id}`} className="payment-list__view-btn">
                        View
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}

function SummaryChip({
  label,
  value,
  tone,
}: {
  label: string
  value: number
  tone: 'neutral' | 'success' | 'warning' | 'danger'
}) {
  return (
    <div className={`payment-list__summary-chip payment-list__summary-chip--${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}
