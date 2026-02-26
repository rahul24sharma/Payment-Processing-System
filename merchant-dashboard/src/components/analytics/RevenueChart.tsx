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
          <LineChart data={chartData}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="date" />
          <YAxis
            yAxisId="left"
            tickFormatter={(value) => `$${value.toLocaleString()}`}
          />
          <YAxis yAxisId="right" orientation="right" />
          <Tooltip
            formatter={(value, name) => {
              const numericValue = typeof value === 'number' ? value : Number(value)
              if (name === 'revenue') {
                return [formatCurrency((Number.isFinite(numericValue) ? numericValue : 0) * 100), 'Revenue']
              }
              return [String(value), 'Payments']
            }}
          />
          <Legend />
          <Line
            yAxisId="left"
            type="monotone"
            dataKey="revenue"
            stroke="#16a34a"
            strokeWidth={3}
            dot={false}
            activeDot={{ r: 4 }}
            name="Revenue"
          />
          <Line
            yAxisId="right"
            type="monotone"
            dataKey="payments"
            stroke="#165dff"
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
