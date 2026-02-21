import { usePayment, useCapturePayment, useRefundPayment } from '@/hooks/usePayments'
import { formatCurrency, formatDate, formatStatus } from '@/utils/formatters'
import { useState } from 'react'

interface Props {
  paymentId: string
}

export default function PaymentDetail({ paymentId }: Props) {
  const { data: payment, isLoading, error } = usePayment(paymentId)
  const capturePayment = useCapturePayment()
  const refundPayment = useRefundPayment()
  
  const [refundAmount, setRefundAmount] = useState('')
  const [refundReason, setRefundReason] = useState('customer_request')
  
  if (isLoading) {
    return <div>Loading payment details...</div>
  }
  
  if (error) {
    return <div style={{ color: 'red' }}>Error: {error.message}</div>
  }
  
  if (!payment) {
    return <div>Payment not found</div>
  }
  
  const handleCapture = async () => {
    if (!confirm('Are you sure you want to capture this payment?')) return
    
    try {
      await capturePayment.mutateAsync({ id: paymentId })
      alert('Payment captured successfully!')
    } catch (error: any) {
      alert(`Error: ${error.response?.data?.error?.message || error.message}`)
    }
  }
  
  const handleRefund = async () => {
    if (!confirm(`Refund ${refundAmount ? formatCurrency(parseFloat(refundAmount) * 100) : 'full amount'}?`)) return
    
    try {
      await refundPayment.mutateAsync({
        id: paymentId,
        request: {
          amount: refundAmount ? Math.round(parseFloat(refundAmount) * 100) : undefined,
          reason: refundReason,
        },
      })
      alert('Refund created successfully!')
      setRefundAmount('')
    } catch (error: any) {
      alert(`Error: ${error.response?.data?.error?.message || error.message}`)
    }
  }
  
  return (
    <div style={{ margin: '20px', maxWidth: '800px' }}>
      <h2>Payment Details</h2>
      
      {/* Payment Info */}
      <div style={{ border: '1px solid #ddd', padding: '20px', marginBottom: '20px' }}>
        <h3>Basic Information</h3>
        
        <table style={{ width: '100%' }}>
          <tbody>
            <tr>
              <td style={{ padding: '8px', fontWeight: 'bold' }}>Payment ID:</td>
              <td style={{ padding: '8px' }}>{payment.id}</td>
            </tr>
            <tr>
              <td style={{ padding: '8px', fontWeight: 'bold' }}>Amount:</td>
              <td style={{ padding: '8px', fontSize: '18px', fontWeight: 'bold' }}>
                {formatCurrency(payment.amount, payment.currency)}
              </td>
            </tr>
            <tr>
              <td style={{ padding: '8px', fontWeight: 'bold' }}>Status:</td>
              <td style={{ padding: '8px' }}>
                <span
                  style={{
                    padding: '6px 12px',
                    borderRadius: '4px',
                    background: getStatusBackground(payment.status),
                    color: 'white',
                  }}
                >
                  {formatStatus(payment.status)}
                </span>
              </td>
            </tr>
            <tr>
              <td style={{ padding: '8px', fontWeight: 'bold' }}>Customer:</td>
              <td style={{ padding: '8px' }}>
                {payment.customer?.name && <div>{payment.customer.name}</div>}
                <div>{payment.customer?.email}</div>
              </td>
            </tr>
            <tr>
              <td style={{ padding: '8px', fontWeight: 'bold' }}>Created:</td>
              <td style={{ padding: '8px' }}>{formatDate(payment.createdAt)}</td>
            </tr>
            {payment.capturedAt && (
              <tr>
                <td style={{ padding: '8px', fontWeight: 'bold' }}>Captured:</td>
                <td style={{ padding: '8px' }}>{formatDate(payment.capturedAt)}</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      
      {/* Fraud Details */}
      {payment.fraudDetails && (
        <div style={{ border: '1px solid #ddd', padding: '20px', marginBottom: '20px' }}>
          <h3>Fraud Assessment</h3>
          <table style={{ width: '100%' }}>
            <tbody>
              <tr>
                <td style={{ padding: '8px', fontWeight: 'bold' }}>Fraud Score:</td>
                <td style={{ padding: '8px' }}>
                  {payment.fraudDetails.score} / 100
                </td>
              </tr>
              <tr>
                <td style={{ padding: '8px', fontWeight: 'bold' }}>Risk Level:</td>
                <td style={{ padding: '8px' }}>
                  <span
                    style={{
                      padding: '4px 8px',
                      borderRadius: '4px',
                      background: getRiskBackground(payment.fraudDetails.riskLevel),
                      color: 'white',
                    }}
                  >
                    {payment.fraudDetails.riskLevel.toUpperCase()}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      )}
      
      {/* Actions */}
      <div style={{ border: '1px solid #ddd', padding: '20px', marginBottom: '20px' }}>
        <h3>Actions</h3>
        
        {payment.status === 'authorized' && (
          <button
            onClick={handleCapture}
            disabled={capturePayment.isPending}
            style={{
              padding: '10px 20px',
              background: '#4caf50',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer',
              marginRight: '10px',
            }}
          >
            {capturePayment.isPending ? 'Capturing...' : 'Capture Payment'}
          </button>
        )}
        
        {(payment.status === 'captured' || payment.status === 'partially_refunded') && (
          <div style={{ marginTop: '20px' }}>
            <h4>Create Refund</h4>
            <div style={{ marginBottom: '10px' }}>
              <label>
                Refund Amount ($) - Leave empty for full refund:
                <input
                  type="number"
                  step="0.01"
                  value={refundAmount}
                  onChange={(e) => setRefundAmount(e.target.value)}
                  placeholder={`Max: ${(payment.amount / 100).toFixed(2)}`}
                  style={{ width: '200px', padding: '8px', marginLeft: '10px' }}
                />
              </label>
            </div>
            
            <div style={{ marginBottom: '10px' }}>
              <label>
                Reason:
                <select
                  value={refundReason}
                  onChange={(e) => setRefundReason(e.target.value)}
                  style={{ marginLeft: '10px', padding: '8px' }}
                >
                  <option value="customer_request">Customer Request</option>
                  <option value="duplicate">Duplicate</option>
                  <option value="fraudulent">Fraudulent</option>
                  <option value="other">Other</option>
                </select>
              </label>
            </div>
            
            <button
              onClick={handleRefund}
              disabled={refundPayment.isPending}
              style={{
                padding: '10px 20px',
                background: '#ff9800',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer',
              }}
            >
              {refundPayment.isPending ? 'Processing...' : 'Create Refund'}
            </button>
          </div>
        )}
      </div>
      
      {/* Refunds */}
      {payment.refunds && payment.refunds.length > 0 && (
        <div style={{ border: '1px solid #ddd', padding: '20px' }}>
          <h3>Refunds</h3>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ background: '#f5f5f5' }}>
                <th style={{ padding: '10px', textAlign: 'left', border: '1px solid #ddd' }}>
                  Refund ID
                </th>
                <th style={{ padding: '10px', textAlign: 'left', border: '1px solid #ddd' }}>
                  Amount
                </th>
                <th style={{ padding: '10px', textAlign: 'left', border: '1px solid #ddd' }}>
                  Status
                </th>
                <th style={{ padding: '10px', textAlign: 'left', border: '1px solid #ddd' }}>
                  Reason
                </th>
                <th style={{ padding: '10px', textAlign: 'left', border: '1px solid #ddd' }}>
                  Created
                </th>
              </tr>
            </thead>
            <tbody>
              {payment.refunds.map((refund) => (
                <tr key={refund.id}>
                  <td style={{ padding: '10px', border: '1px solid #ddd' }}>
                    {refund.id.substring(0, 8)}...
                  </td>
                  <td style={{ padding: '10px', border: '1px solid #ddd' }}>
                    {formatCurrency(refund.amount, refund.currency)}
                  </td>
                  <td style={{ padding: '10px', border: '1px solid #ddd' }}>
                    {formatStatus(refund.status)}
                  </td>
                  <td style={{ padding: '10px', border: '1px solid #ddd' }}>
                    {refund.reason || 'N/A'}
                  </td>
                  <td style={{ padding: '10px', border: '1px solid #ddd' }}>
                    {formatDate(refund.createdAt)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
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