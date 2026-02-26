import { usePaymentStats } from '@/hooks/useAnalytics'
import { formatCurrency } from '@/utils/formatters'
import './AnalyticsWidgets.css'

interface Props {
  days: number
}

export default function StatsCards({ days }: Props) {
  const { data: stats, isLoading, error } = usePaymentStats(days)

  if (isLoading) {
    return <div className="analytics-stats__loading">Loading KPI tiles...</div>
  }

  if (error) {
    return <div className="analytics-stats__error">Error loading analytics stats</div>
  }

  if (!stats) {
    return null
  }

  const successRate =
    stats.totalCount > 0 ? (stats.capturedCount / stats.totalCount) * 100 : 0

  const cards = [
    {
      title: 'Total Revenue',
      value: formatCurrency(stats.totalAmount),
      subtitle: `${stats.capturedCount} captured payments`,
      tone: 'emerald',
      icon: '$',
    },
    {
      title: 'Success Rate',
      value: `${successRate.toFixed(1)}%`,
      subtitle: `${stats.capturedCount} / ${stats.totalCount} payments`,
      tone: 'blue',
      icon: '%',
    },
    {
      title: 'Average Payment',
      value: formatCurrency(stats.averageAmount),
      subtitle: `Across ${stats.totalCount} payments`,
      tone: 'cyan',
      icon: 'AVG',
    },
    {
      title: 'Failed Payments',
      value: stats.failedCount + stats.declinedCount,
      subtitle: `${((stats.failedCount + stats.declinedCount) / (stats.totalCount || 1) * 100).toFixed(1)}% failure rate`,
      tone: 'rose',
      icon: '!',
    },
  ]

  return (
    <div className="analytics-stats">
      {cards.map((card) => (
        <div key={card.title} className={`analytics-stats__card analytics-stats__card--${card.tone}`}>
          <div className="analytics-stats__head">
            <div className="analytics-stats__title">{card.title}</div>
            <div className="analytics-stats__icon" aria-hidden>
              {card.icon}
            </div>
          </div>

          <div className="analytics-stats__value">{card.value}</div>
          <div className="analytics-stats__subtitle">{card.subtitle}</div>
        </div>
      ))}
    </div>
  )
}
