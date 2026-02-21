import { usePayments } from '@/hooks/usePayments'
import { formatCurrency, formatDate, formatStatus } from '@/utils/formatters'

export default function DashboardPage() {
  const { data, isLoading } = usePayments()
  
  if (isLoading) {
    return <div>Loading dashboard...</div>
  }
  
  const payments = data?.data || []
  
  // Calculate stats
  const totalPayments = payments.length
  const capturedPayments = payments.filter((p) => p.status === 'captured')
  const totalRevenue = capturedPayments.reduce((sum, p) => sum + p.amount, 0)
  const failedPayments = payments.filter(
    (p) => p.status === 'failed' || p.status === 'declined'
  )
  
  return (
    <div style={{ margin: '20px' }}>
      <h1>Dashboard</h1>
      
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '20px', marginTop: '20px' }}>
        {/* Total Payments */}
        <div style={{ border: '1px solid #ddd', padding: '20px', borderRadius: '8px' }}>
          <div style={{ fontSize: '14px', color: '#666' }}>Total Payments</div>
          <div style={{ fontSize: '32px', fontWeight: 'bold', marginTop: '10px' }}>
            {totalPayments}
          </div>
        </div>
        
        {/* Total Revenue */}
        <div style={{ border: '1px solid #ddd', padding: '20px', borderRadius: '8px' }}>
          <div style={{ fontSize: '14px', color: '#666' }}>Total Revenue</div>
          <div style={{ fontSize: '32px', fontWeight: 'bold', marginTop: '10px', color: '#4caf50' }}>
            {formatCurrency(totalRevenue)}
          </div>
        </div>
        
        {/* Success Rate */}
        <div style={{ border: '1px solid #ddd', padding: '20px', borderRadius: '8px' }}>
          <div style={{ fontSize: '14px', color: '#666' }}>Success Rate</div>
          <div style={{ fontSize: '32px', fontWeight: 'bold', marginTop: '10px', color: '#2196f3' }}>
            {totalPayments > 0
              ? ((capturedPayments.length / totalPayments) * 100).toFixed(1)
              : 0}
            %
          </div>
        </div>
        
        {/* Failed Payments */}
        <div style={{ border: '1px solid #ddd', padding: '20px', borderRadius: '8px' }}>
          <div style={{ fontSize: '14px', color: '#666' }}>Failed</div>
          <div style={{ fontSize: '32px', fontWeight: 'bold', marginTop: '10px', color: '#f44336' }}>
            {failedPayments.length}
          </div>
        </div>
      </div>
      
      {/* Quick Actions */}
      <div style={{ marginTop: '40px' }}>
        <h3>Quick Actions</h3>
        <div style={{ display: 'flex', gap: '10px', marginTop: '15px' }}>
          <button
            onClick={() => (window.location.href = '/create-payment')}
            style={{
              padding: '12px 24px',
              background: '#007bff',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer',
              fontSize: '16px',
            }}
          >
            Create Payment
          </button>
          
          <button
            onClick={() => (window.location.href = '/payments')}
            style={{
              padding: '12px 24px',
              background: '#6c757d',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer',
              fontSize: '16px',
            }}
          >
            View All Payments
          </button>
        </div>
      </div>
      
      {/* Recent Payments */}
      <div style={{ marginTop: '40px' }}>
        <h3>Recent Payments</h3>
        <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: '15px' }}>
          <thead>
            <tr style={{ background: '#f5f5f5' }}>
              <th style={{ padding: '10px', textAlign: 'left', border: '1px solid #ddd' }}>ID</th>
              <th style={{ padding: '10px', textAlign: 'left', border: '1px solid #ddd' }}>
                Amount
              </th>
              <th style={{ padding: '10px', textAlign: 'left', border: '1px solid #ddd' }}>
                Status
              </th>
              <th style={{ padding: '10px', textAlign: 'left', border: '1px solid #ddd' }}>
                Customer
              </th>
              <th style={{ padding: '10px', textAlign: 'left', border: '1px solid #ddd' }}>
                Created
              </th>
            </tr>
          </thead>
          <tbody>
            {payments.slice(0, 5).map((payment) => (
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
                  {formatStatus(payment.status)}
                </td>
                <td style={{ padding: '10px', border: '1px solid #ddd' }}>
                  {payment.customer?.email || 'N/A'}
                </td>
                <td style={{ padding: '10px', border: '1px solid #ddd' }}>
                  {formatDate(payment.createdAt)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
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

function getRiskBackground(riskLevel: string): string {
  switch (riskLevel.toLowerCase()) {
    case 'very_low':
    case 'low':
      return '#4caf50'
    case 'medium':
      return '#ff9800'
    case 'high':
      return '#ff5722'
    case 'critical':
      return '#f44336'
    default:
      return '#9e9e9e'
  }
}