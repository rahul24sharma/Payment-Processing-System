import { usePaymentsByStatus } from '@/hooks/useAnalytics'
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts'
import { formatStatus } from '@/utils/formatters'
import './AnalyticsWidgets.css'

interface Props {
  days: number
}

const STATUS_COLORS: Record<string, string> = {
  captured: '#28a745',
  authorized: '#007bff',
  pending: '#ffc107',
  failed: '#dc3545',
  declined: '#e74c3c',
  refunded: '#6c757d',
  partially_refunded: '#95a5a6',
}

export default function PaymentStatusChart({ days }: Props) {
  const { data, isLoading, error } = usePaymentsByStatus(days)

  if (isLoading) {
    return <div className="analytics-widget__loading">Loading payment status chart...</div>
  }

  if (error) {
    return <div className="analytics-widget__error">Error loading payment status chart</div>
  }

  if (!data || data.length === 0) {
    return <div className="analytics-widget__empty">No payment data available</div>
  }

  const chartData = data.map((item) => ({
    name: formatStatus(item.status),
    value: item.count,
    percentLabel: `${item.percentage.toFixed(1)}%`,
    rawStatus: item.status,
  }))

  return (
    <div className="analytics-widget">
      <div className="analytics-widget__header">
        <div>
          <p className="analytics-widget__eyebrow">Mix Analysis</p>
          <h3 className="analytics-widget__title">Payments by Status</h3>
          <p className="analytics-widget__subtitle">Distribution of payment outcomes in the selected range</p>
        </div>
        <div className="analytics-widget__badge">{data.reduce((sum, item) => sum + item.count, 0)} payments</div>
      </div>

      <div className="analytics-widget__chart-frame">
        <div className="analytics-pie-wrap">
          <ResponsiveContainer width="100%" height={280}>
          <PieChart>
            <Pie
              data={chartData}
              cx="50%"
              cy="50%"
              labelLine={false}
              outerRadius={96}
              innerRadius={62}
              paddingAngle={3}
              fill="#8884d8"
              dataKey="value"
            >
              {chartData.map((entry, index) => (
              <Cell
                key={`cell-${index}`}
                fill={STATUS_COLORS[entry.rawStatus] || '#999'}
              />
            ))}
            </Pie>
            <Tooltip
              formatter={(value, _name, payload: any) => [
                `${String(value)} (${payload?.payload?.percentLabel ?? ''})`,
                'Payments',
              ]}
              contentStyle={{ borderRadius: 12, border: '1px solid rgba(15,23,42,0.12)', boxShadow: '0 12px 28px rgba(15,23,42,0.12)' }}
              labelStyle={{ color: '#0f172a', fontWeight: 700 }}
            />
          </PieChart>
        </ResponsiveContainer>
          <div className="analytics-pie-center">
            <strong>{data.reduce((sum, item) => sum + item.count, 0)}</strong>
            <span>Total</span>
          </div>
        </div>
      </div>

      <div className="analytics-status-legend">
        {chartData.map((item) => (
          <div key={item.rawStatus} className="analytics-status-legend__item">
            <div className="analytics-status-legend__meta">
              <span
                className="analytics-status-legend__dot"
                style={{ background: STATUS_COLORS[item.rawStatus] || '#999' }}
              />
              <span className="analytics-status-legend__name">{item.name}</span>
            </div>
            <span className="analytics-status-legend__value">
              {item.value} • {item.percentLabel}
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}
