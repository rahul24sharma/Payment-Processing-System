import { useTopCustomers } from '@/hooks/useAnalytics'
import { formatCurrency } from '@/utils/formatters'
import './AnalyticsWidgets.css'

interface Props {
  days: number
}

export default function TopCustomersTable({ days }: Props) {
  const { data: customers, isLoading, error } = useTopCustomers(days, 10)

  if (isLoading) {
    return <div className="analytics-widget__loading">Loading top customers...</div>
  }

  if (error) {
    return <div className="analytics-widget__error">Error loading top customers</div>
  }

  if (!customers || customers.length === 0) {
    return <div className="analytics-widget__empty">No customer data available</div>
  }

  return (
    <div className="analytics-top-customers">
      <div className="analytics-widget__header">
        <div>
          <p className="analytics-widget__eyebrow">Customer Revenue</p>
          <h3 className="analytics-widget__title">Top Customers by Revenue</h3>
          <p className="analytics-widget__subtitle">Highest-value captured-payment customers in the selected range</p>
        </div>
        <div className="analytics-widget__badge">Top {customers.length}</div>
      </div>

      <div className="analytics-top-customers__table-wrap">
        <table className="analytics-top-customers__table">
          <thead>
            <tr>
              <th>Rank</th>
              <th>Customer</th>
              <th>Total Spent</th>
              <th>Payments</th>
              <th>Avg. Amount</th>
            </tr>
          </thead>
          <tbody>
            {customers.map((customer, index) => (
              <tr key={customer.customerId}>
                <td className="analytics-top-customers__rank">
                  {index === 0 && '🥇'}
                  {index === 1 && '🥈'}
                  {index === 2 && '🥉'}
                  {index > 2 && `${index + 1}`}
                </td>
                <td>
                  <div className="analytics-top-customers__customer">
                    <span className="analytics-top-customers__email">{customer.email}</span>
                    <span className="analytics-top-customers__id">{customer.customerId}</span>
                  </div>
                </td>
                <td className="analytics-top-customers__money">{formatCurrency(customer.totalSpent)}</td>
                <td>{customer.paymentCount}</td>
                <td>{formatCurrency(customer.totalSpent / customer.paymentCount)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
