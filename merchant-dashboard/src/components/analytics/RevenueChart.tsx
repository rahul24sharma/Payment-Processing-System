import { useRevenueByDate } from '@/hooks/useAnalytics'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts'
import { formatCurrency } from '@/utils/formatters'
import './AnalyticsWidgets.css'

interface Props {
  days: number
}

export default function RevenueChart({ days }: Props) {
  const { data, isLoading, error } = useRevenueByDate(days)

  if (isLoading) {
    return <div className="analytics-widget__loading">Loading revenue chart...</div>
  }

  if (error) {
    return <div className="analytics-widget__error">Error loading revenue chart</div>
  }

  if (!data || data.length === 0) {
    return <div className="analytics-widget__empty">No revenue data available for this period</div>
  }

  // Format data for chart
  const chartData = data.map((item) => ({
    date: new Date(item.date).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
    revenue: item.revenue / 100, // Convert cents to dollars
    payments: item.paymentCount,
  }))

  return (
    <div className="analytics-widget">
      <div className="analytics-widget__header">
        <div>
          <p className="analytics-widget__eyebrow">Revenue Trend</p>
          <h3 className="analytics-widget__title">Revenue Over Time</h3>
          <p className="analytics-widget__subtitle">Captured revenue and payment volume across the selected period</p>
        </div>
        <div className="analytics-widget__badge">{chartData.length} points</div>
      </div>

      <div className="analytics-widget__chart-frame">
        <ResponsiveContainer width="100%" height={320}>
          <LineChart data={chartData} margin={{ top: 12, right: 10, left: 0, bottom: 8 }}>
          <CartesianGrid strokeDasharray="4 6" vertical={false} stroke="rgba(100, 116, 139, 0.25)" />
          <XAxis
            dataKey="date"
            tick={{ fill: '#64748b', fontSize: 11 }}
            axisLine={false}
            tickLine={false}
          />
          <YAxis
            yAxisId="left"
            tickFormatter={(value) => `$${value.toLocaleString()}`}
            tick={{ fill: '#64748b', fontSize: 11 }}
            axisLine={false}
            tickLine={false}
          />
          <YAxis
            yAxisId="right"
            orientation="right"
            tick={{ fill: '#94a3b8', fontSize: 11 }}
            axisLine={false}
            tickLine={false}
          />
          <Tooltip
            formatter={(value, name) => {
              const numericValue = typeof value === 'number' ? value : Number(value)
              if (name === 'revenue') {
                return [formatCurrency((Number.isFinite(numericValue) ? numericValue : 0) * 100), 'Revenue']
              }
              return [String(value), 'Payments']
            }}
            contentStyle={{ borderRadius: 12, border: '1px solid rgba(15,23,42,0.12)', boxShadow: '0 12px 28px rgba(15,23,42,0.12)' }}
            labelStyle={{ color: '#0f172a', fontWeight: 700 }}
          />
          <Legend wrapperStyle={{ fontSize: 12, color: '#475569' }} />
          <Line
            yAxisId="left"
            type="monotone"
            dataKey="revenue"
            stroke="#0ea5a4"
            strokeWidth={3}
            dot={false}
            activeDot={{ r: 5, strokeWidth: 0, fill: '#0ea5a4' }}
            name="Revenue"
          />
          <Line
            yAxisId="right"
            type="monotone"
            dataKey="payments"
            stroke="#2563eb"
            strokeWidth={2}
            dot={false}
            name="Payment Count"
          />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}
