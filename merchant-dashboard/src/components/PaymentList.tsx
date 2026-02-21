import { usePayments } from '@/hooks/usePayments'
import { formatCurrency, formatDate, formatStatus } from '@/utils/formatters'
import { useState } from 'react'

export default function PaymentList() {
  const [statusFilter, setStatusFilter] = useState<string>('')
  const { data, isLoading, error } = usePayments(statusFilter || undefined)
  
  if (isLoading) {
    return <div>Loading payments...</div>
  }
  
  if (error) {
    return <div style={{ color: 'red' }}>Error loading payments: {error.message}</div>
  }
  
  const payments = data?.data || []
  
  return (
    <div style={{ margin: '20px' }}>
      <h2>Payments</h2>
      
      <div style={{ marginBottom: '20px' }}>
        <label>
          Filter by Status:
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            style={{ marginLeft: '10px', padding: '5px' }}
          >
            <option value="">All</option>
            <option value="pending">Pending</option>
            <option value="authorized">Authorized</option>
            <option value="captured">Captured</option>
            <option value="failed">Failed</option>
            <option value="declined">Declined</option>
          </select>
        </label>
      </div>
      
      {payments.length === 0 ? (
        <p>No payments found.</p>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ background: '#f5f5f5' }}>
              <th style={{ padding: '10px', textAlign: 'left', border: '1px solid #ddd' }}>ID</th>
              <th style={{ padding: '10px', textAlign: 'left', border: '1px solid #ddd' }}>Amount</th>
              <th style={{ padding: '10px', textAlign: 'left', border: '1px solid #ddd' }}>Status</th>
              <th style={{ padding: '10px', textAlign: 'left', border: '1px solid #ddd' }}>Customer</th>
              <th style={{ padding: '10px', textAlign: 'left', border: '1px solid #ddd' }}>Created</th>
              <th style={{ padding: '10px', textAlign: 'left', border: '1px solid #ddd' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {payments.map((payment) => (
              <tr key={payment.id}>
                <td style={{ padding: '10px', border: '1px solid #ddd' }}>
                  <a href={`/payments/${payment.id}`} style={{ color: '#007bff' }}>
                    {payment.id.substring(0, 8)}...
                  </a>
                </td>
                <td style={{ padding: '10px', border: '1px solid #ddd' }}>
                  {formatCurrency(payment.amount, payment.currency)}
                </td>
                <td style={{ padding: '10px', border: '1px solid #ddd' }}>
                  <span
                    style={{
                      padding: '4px 8px',
                      borderRadius: '4px',
                      background: getStatusBackground(payment.status),
                      color: 'white',
                      fontSize: '12px',
                    }}
                  >
                    {formatStatus(payment.status)}
                  </span>
                </td>
                <td style={{ padding: '10px', border: '1px solid #ddd' }}>
                  {payment.customer?.email || 'N/A'}
                </td>
                <td style={{ padding: '10px', border: '1px solid #ddd' }}>
                  {formatDate(payment.createdAt)}
                </td>
                <td style={{ padding: '10px', border: '1px solid #ddd' }}>
                  <button
                    onClick={() => window.location.href = `/payments/${payment.id}`}
                    style={{
                      padding: '5px 10px',
                      background: '#007bff',
                      color: 'white',
                      border: 'none',
                      borderRadius: '4px',
                      cursor: 'pointer',
                    }}
                  >
                    View
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      
      <div style={{ marginTop: '20px' }}>
        <p>Total: {payments.length} payments</p>
      </div>
    </div>
  )
}

function getStatusBackground(status: string): string {
  switch (status.toLowerCase()) {
    case 'captured':
      return '#4caf50'
    case 'authorized':
      return '#2196f3'
    case 'pending':
      return '#ff9800'
    case 'failed':
    case 'declined':
      return '#f44336'
    default:
      return '#9e9e9e'
  }
}