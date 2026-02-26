import { usePaymentStats } from '@/hooks/useAnalytics'
import './AnalyticsWidgets.css'

interface Props {
  days: number
}

export default function PaymentFunnelChart({ days }: Props) {
  const { data: stats, isLoading } = usePaymentStats(days)

  if (isLoading || !stats) {
    return <div className="analytics-widget__loading">Loading payment funnel...</div>
  }

  const funnelSteps = [
    { label: 'Created', count: stats.totalCount, color: 'linear-gradient(135deg, #165dff, #3b82f6)' },
    {
      label: 'Authorized',
      count: stats.authorizedCount + stats.capturedCount,
      color: 'linear-gradient(135deg, #0891b2, #06b6d4)',
    },
    { label: 'Captured', count: stats.capturedCount, color: 'linear-gradient(135deg, #16a34a, #22c55e)' },
  ]

  const maxCount = Math.max(...funnelSteps.map((s) => s.count))

  return (
    <div className="analytics-widget">
      <div className="analytics-widget__header">
        <div>
          <p className="analytics-widget__eyebrow">Conversion Funnel</p>
          <h3 className="analytics-widget__title">Payment Funnel</h3>
          <p className="analytics-widget__subtitle">Where volume is retained or lost from create to capture</p>
        </div>
        <div className="analytics-widget__badge">
          {stats.totalCount > 0 ? ((stats.capturedCount / stats.totalCount) * 100).toFixed(1) : '0.0'}% capture rate
        </div>
      </div>

      <div className="analytics-funnel">
        {funnelSteps.map((step, index) => {
          const percentage = maxCount > 0 ? (step.count / maxCount) * 100 : 0
          const dropoff = index > 0 ? funnelSteps[index - 1].count - step.count : 0

          return (
            <div key={step.label} className="analytics-funnel__step">
              <div className="analytics-funnel__meta">
                <span>{step.label}</span>
                <span>{step.count.toLocaleString()} payments</span>
              </div>

              <div className="analytics-funnel__bar-shell">
                <div
                  className="analytics-funnel__bar"
                  style={{ width: `${Math.max(percentage, 6)}%`, background: step.color }}
                >
                  {percentage.toFixed(0)}%
                </div>
              </div>

              {dropoff > 0 && <div className="analytics-funnel__dropoff">↓ {dropoff} dropped off</div>}
            </div>
          )
        })}
      </div>

      <div className="analytics-widget__footnote">
        <strong>Conversion Rate:</strong>{' '}
        {stats.totalCount > 0
          ? ((stats.capturedCount / stats.totalCount) * 100).toFixed(1)
          : 0}
        % of payments are successfully captured
      </div>
    </div>
  )
}
