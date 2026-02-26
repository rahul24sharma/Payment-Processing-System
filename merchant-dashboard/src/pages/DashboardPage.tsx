import { Link } from 'react-router-dom'
import { usePayments } from '@/hooks/usePayments'
import AsyncState from '@/components/ui/AsyncState'
import { formatCurrency, formatDate, formatStatus } from '@/utils/formatters'
import './DashboardPage.css'

type PaymentStatusBucket =
  | 'captured'
  | 'authorized'
  | 'pending'
  | 'failed'
  | 'declined'
  | 'refunded'
  | 'partially_refunded'

export default function DashboardPage() {
  const { data, isLoading } = usePayments()

  if (isLoading) {
    return (
      <AsyncState
        kind="loading"
        title="Loading dashboard"
        message="Fetching recent payments, refunds, and revenue metrics."
      />
    )
  }

  const payments = data?.data || []
  const totalPayments = payments.length
  const capturedPayments = payments.filter((p) => p.status === 'captured')
  const totalRevenue = capturedPayments.reduce((sum, p) => sum + p.amount, 0)
  const failedPayments = payments.filter((p) => p.status === 'failed' || p.status === 'declined')
  const refundedAmount = payments
    .flatMap((p) => p.refunds || [])
    .filter((r) => r.status === 'succeeded')
    .reduce((sum, r) => sum + r.amount, 0)

  const successRate =
    totalPayments > 0 ? Number(((capturedPayments.length / totalPayments) * 100).toFixed(1)) : 0

  const statusBuckets: Array<{ label: string; key: PaymentStatusBucket; count: number }> = [
    { label: 'Captured', key: 'captured', count: payments.filter((p) => p.status === 'captured').length },
    { label: 'Authorized', key: 'authorized', count: payments.filter((p) => p.status === 'authorized').length },
    { label: 'Pending', key: 'pending', count: payments.filter((p) => p.status === 'pending').length },
    {
      label: 'Refunded',
      key: 'refunded',
      count: payments.filter((p) => p.status === 'refunded' || p.status === 'partially_refunded').length,
    },
    { label: 'Failed', key: 'failed', count: failedPayments.length },
  ]

  const highestBucket = Math.max(...statusBuckets.map((b) => b.count), 1)
  const recentPayments = payments.slice(0, 6)

  return (
    <div className="dashboard">
      <section className="dashboard-hero">
        <div>
          <p className="dashboard-eyebrow">Merchant Overview</p>
          <h1>Payments command center</h1>
          <p className="dashboard-subtitle">
            Track recent payment performance, jump into refunds, and monitor operational status from one place.
          </p>
        </div>
        <div className="dashboard-hero__actions">
          <Link className="dashboard-btn dashboard-btn--primary" to="/create-payment">
            New Payment
          </Link>
          <Link className="dashboard-btn dashboard-btn--secondary" to="/payments">
            View Payments
          </Link>
        </div>
      </section>

      <section className="dashboard-grid dashboard-grid--stats">
        <KpiCard
          label="Total Payments"
          value={String(totalPayments)}
          tone="neutral"
          helper={`${capturedPayments.length} captured`}
        />
        <KpiCard
          label="Captured Revenue"
          value={formatCurrency(totalRevenue)}
          tone="success"
          helper={`${successRate}% success rate`}
        />
        <KpiCard
          label="Refunded Amount"
          value={formatCurrency(refundedAmount)}
          tone="warning"
          helper="Successful refunds only"
        />
        <KpiCard
          label="Failed / Declined"
          value={String(failedPayments.length)}
          tone="danger"
          helper="Needs retry / review"
        />
      </section>

      <section className="dashboard-grid dashboard-grid--main">
        <div className="dashboard-card">
          <div className="dashboard-card__header">
            <div>
              <p className="dashboard-card__eyebrow">Status Mix</p>
              <h2>Payment pipeline</h2>
            </div>
            <span className="dashboard-pill">Live snapshot</span>
          </div>
          <div className="status-list">
            {statusBuckets.map((bucket) => (
              <div className="status-row" key={bucket.label}>
                <div className="status-row__label">
                  <span className={`status-dot status-dot--${bucket.key}`} />
                  <span>{bucket.label}</span>
                </div>
                <div className="status-row__bar-wrap">
                  <div
                    className={`status-row__bar status-row__bar--${bucket.key}`}
                    style={{ width: `${Math.max((bucket.count / highestBucket) * 100, bucket.count > 0 ? 10 : 0)}%` }}
                  />
                </div>
                <div className="status-row__count">{bucket.count}</div>
              </div>
            ))}
          </div>
        </div>

        <div className="dashboard-card dashboard-card--action-panel">
          <div className="dashboard-card__header">
            <div>
              <p className="dashboard-card__eyebrow">Quick Actions</p>
              <h2>Operational shortcuts</h2>
            </div>
          </div>
          <div className="action-grid">
            <Link to="/create-payment" className="action-tile action-tile--blue">
              <span className="action-tile__icon">+</span>
              <div>
                <div className="action-tile__title">Create Payment</div>
                <div className="action-tile__desc">Start a new card charge flow</div>
              </div>
            </Link>
            <Link to="/refunds" className="action-tile action-tile--amber">
              <span className="action-tile__icon">↩</span>
              <div>
                <div className="action-tile__title">Refunds</div>
                <div className="action-tile__desc">Track and issue refunds</div>
              </div>
            </Link>
            <Link to="/customers" className="action-tile action-tile--teal">
              <span className="action-tile__icon">👥</span>
              <div>
                <div className="action-tile__title">Customers</div>
                <div className="action-tile__desc">Review payer history</div>
              </div>
            </Link>
            <Link to="/analytics" className="action-tile action-tile--slate">
              <span className="action-tile__icon">↗</span>
              <div>
                <div className="action-tile__title">Analytics</div>
                <div className="action-tile__desc">Inspect performance trends</div>
              </div>
            </Link>
          </div>
        </div>
      </section>

      <section className="dashboard-card">
        <div className="dashboard-card__header">
          <div>
            <p className="dashboard-card__eyebrow">Recent Activity</p>
            <h2>Latest payments</h2>
          </div>
          <Link to="/payments" className="dashboard-link">
            View all
          </Link>
        </div>

        {recentPayments.length === 0 ? (
          <AsyncState
            kind="empty"
            title="No payments yet"
            message="Create your first payment to start populating this dashboard."
            action={
              <Link className="dashboard-btn dashboard-btn--primary" to="/create-payment">
                Create Payment
              </Link>
            }
          />
        ) : (
          <div className="dashboard-table-wrap">
            <table className="dashboard-table">
              <thead>
                <tr>
                  <th>Payment</th>
                  <th>Customer</th>
                  <th>Amount</th>
                  <th>Status</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                {recentPayments.map((payment) => (
                  <tr key={payment.id}>
                    <td>
                      <Link to={`/payments/${payment.id}`} className="dashboard-table__id">
                        {payment.id.slice(0, 8)}...
                      </Link>
                    </td>
                    <td>{payment.customer?.email || 'N/A'}</td>
                    <td className="dashboard-table__amount">
                      {formatCurrency(payment.amount, payment.currency)}
                    </td>
                    <td>
                      <span className={`dashboard-status dashboard-status--${payment.status}`}>
                        {formatStatus(payment.status)}
                      </span>
                    </td>
                    <td>{formatDate(payment.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}

function KpiCard({
  label,
  value,
  helper,
  tone,
}: {
  label: string
  value: string
  helper: string
  tone: 'neutral' | 'success' | 'warning' | 'danger'
}) {
  return (
    <div className={`kpi-card kpi-card--${tone}`}>
      <div className="kpi-card__label">{label}</div>
      <div className="kpi-card__value">{value}</div>
      <div className="kpi-card__helper">{helper}</div>
    </div>
  )
}
