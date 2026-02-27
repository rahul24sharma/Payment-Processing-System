import { useFraudScoreDistribution } from '@/hooks/useAnalytics'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from 'recharts'
import './AnalyticsWidgets.css'

interface Props {
  days: number
}

const RISK_COLORS = ['#28a745', '#5cb85c', '#ffc107', '#ff5722', '#dc3545']

export default function FraudScoreChart({ days }: Props) {
  const { data, isLoading, error } = useFraudScoreDistribution(days)

  if (isLoading) {
    return <div className="analytics-widget__loading">Loading fraud distribution...</div>
  }

  if (error) {
    return <div className="analytics-widget__error">Error loading fraud chart</div>
  }

  if (!data || data.every((d) => d.count === 0)) {
    return <div className="analytics-widget__empty">No fraud data available</div>
  }

  const total = data.reduce((sum, d) => sum + d.count, 0)

  return (
    <div className="analytics-widget">
      <div className="analytics-widget__header">
        <div>
          <p className="analytics-widget__eyebrow">Risk Profiling</p>
          <h3 className="analytics-widget__title">Fraud Score Distribution</h3>
          <p className="analytics-widget__subtitle">Payment counts grouped by fraud score buckets</p>
        </div>
        <div className="analytics-widget__badge">{total} scored payments</div>
      </div>

      <div className="analytics-widget__chart-frame">
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={data} margin={{ top: 12, right: 8, left: 0, bottom: 8 }}>
          <CartesianGrid strokeDasharray="4 6" vertical={false} stroke="rgba(100, 116, 139, 0.25)" />
          <XAxis
            dataKey="range"
            tick={{ fill: '#64748b', fontSize: 11 }}
            axisLine={false}
            tickLine={false}
          />
          <YAxis
            tick={{ fill: '#64748b', fontSize: 11 }}
            axisLine={false}
            tickLine={false}
          />
          <Tooltip
            formatter={(value) => [`${String(value)} payments`, 'Count']}
            contentStyle={{ borderRadius: 12, border: '1px solid rgba(15,23,42,0.12)', boxShadow: '0 12px 28px rgba(15,23,42,0.12)' }}
            labelStyle={{ color: '#0f172a', fontWeight: 700 }}
          />
          <Bar dataKey="count" name="Payments" radius={[8, 8, 0, 0]}>
            {data.map((_, index) => (
              <Cell key={`cell-${index}`} fill={RISK_COLORS[index]} />
            ))}
          </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>

      <div className="analytics-widget__footnote">
        <strong>Interpretation:</strong> Most payments should cluster in low-risk bands (0-25). A growing share in
        50+ usually indicates risk rule drift or attack traffic.
      </div>
    </div>
  )
}
