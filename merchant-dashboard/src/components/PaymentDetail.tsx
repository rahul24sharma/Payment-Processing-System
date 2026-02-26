import { usePayment, useCapturePayment, useRefundPayment, useCompletePaymentAuthentication } from '@/hooks/usePayments'
import { paymentsApi } from '@/api/payments'
import { useToast } from '@/contexts/ToastContext'
import ConfirmDialog from '@/components/ui/ConfirmDialog'
import AsyncState from '@/components/ui/AsyncState'
import { formatCurrency, formatDate, formatStatus } from '@/utils/formatters'
import { useState } from 'react'
import './PaymentDetail.css'

interface Props {
  paymentId: string
}

export default function PaymentDetail({ paymentId }: Props) {
  const { data: payment, isLoading, error, refetch } = usePayment(paymentId)
  const capturePayment = useCapturePayment()
  const refundPayment = useRefundPayment()
  const completeAuth = useCompletePaymentAuthentication()
  const toast = useToast()
  
  const [refundAmount, setRefundAmount] = useState('')
  const [refundReason, setRefundReason] = useState('customer_request')
  const [authMessage, setAuthMessage] = useState<string | null>(null)
  const [authError, setAuthError] = useState<string | null>(null)
  const [showCaptureConfirm, setShowCaptureConfirm] = useState(false)
  const [showRefundConfirm, setShowRefundConfirm] = useState(false)

  const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

  const shouldContinuePolling = (status?: string) =>
    status === 'pending' || status === 'authorized' || status === 'partially_refunded'

  const toPaymentFlowMessage = (err: any) => {
    const raw = err?.response?.data?.error?.message || err?.response?.data?.message || err?.message || 'Unknown error'
    const lower = String(raw).toLowerCase()
    if (lower.includes('currently unavailable')) return 'Payment service is temporarily unavailable. Please retry in a few seconds.'
    if (lower.includes('timed out') || lower.includes('timeout')) return 'The request timed out. Refresh this page to confirm the latest payment status before retrying.'
    if (lower.includes('authentication failed')) return 'Authentication was not completed. Try resuming authentication again.'
    if (lower.includes('canceled')) return 'Authentication was canceled before completion.'
    return raw
  }

  const waitForStablePaymentState = async () => {
    const timeoutMs = 20_000
    const startedAt = Date.now()
    let latest = await paymentsApi.get(paymentId)

    while (Date.now() - startedAt < timeoutMs && shouldContinuePolling(latest.status)) {
      setAuthMessage(`Waiting for payment status update... current: ${formatStatus(latest.status)}`)
      await sleep(2000)
      latest = await paymentsApi.get(paymentId)
    }

    await refetch()
    return latest
  }
  
  if (isLoading) {
    return (
      <AsyncState
        kind="loading"
        title="Loading payment details"
        message="Fetching payment status, customer info, and available actions."
      />
    )
  }
  
  if (error) {
    return <AsyncState kind="error" title="Unable to load payment" message={error.message} />
  }
  
  if (!payment) {
    return <AsyncState kind="empty" title="Payment not found" message="This payment may have been deleted or you may not have access to it." />
  }
  
  const handleCapture = async () => {
    try {
      const updated = await capturePayment.mutateAsync({ id: paymentId })
      if (shouldContinuePolling(updated.status)) {
        setAuthError(null)
        setAuthMessage('Capture submitted. Waiting for payment status update...')
        await waitForStablePaymentState()
      }
      toast.success('Payment captured successfully!')
      setShowCaptureConfirm(false)
    } catch (error: any) {
      toast.error(toPaymentFlowMessage(error))
    }
  }
  
  const handleRefund = async () => {
    try {
      await refundPayment.mutateAsync({
        id: paymentId,
        request: {
          amount: refundAmount ? Math.round(parseFloat(refundAmount) * 100) : undefined,
          reason: refundReason,
        },
      })
      setAuthError(null)
      setAuthMessage('Refund submitted. Waiting for payment status update...')
      await waitForStablePaymentState()
      toast.success('Refund created successfully!')
      setRefundAmount('')
      setShowRefundConfirm(false)
    } catch (error: any) {
      toast.error(toPaymentFlowMessage(error))
    }
  }

  const isPaymentServiceUnavailableError = (err: any) => {
    const data = err?.response?.data
    return (
      data?.service === 'payment-service' &&
      typeof data?.message === 'string' &&
      data.message.includes('currently unavailable')
    )
  }

  const completeAuthenticationWithRetry = async () => {
    const delays = [0, 1200, 2500, 4000]
    let lastError: any

    for (let attempt = 0; attempt < delays.length; attempt += 1) {
      if (delays[attempt] > 0) {
        setAuthMessage(`Payment service reconnecting... retrying (${attempt + 1}/${delays.length})`)
        await sleep(delays[attempt])
      }

      try {
        return await completeAuth.mutateAsync({ id: paymentId })
      } catch (err: any) {
        lastError = err
        if (!isPaymentServiceUnavailableError(err) || attempt === delays.length - 1) {
          throw err
        }
      }
    }

    throw lastError
  }

  const finalizeWithStripeIfNeeded = async () => {
    setAuthError(null)
    setAuthMessage('Checking payment authentication status...')

    let latestPayment = await completeAuthenticationWithRetry()

    if (latestPayment.nextAction?.type === 'use_stripe_sdk' && latestPayment.nextAction.clientSecret) {
      const publishableKey = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY as string | undefined
      if (!publishableKey) {
        throw new Error('VITE_STRIPE_PUBLISHABLE_KEY is not set. Add it to merchant-dashboard/.env.')
      }

      if (!window.Stripe) {
        throw new Error('Stripe.js did not load. Refresh and try again.')
      }

      setAuthMessage('Additional authentication required. Opening Stripe authentication...')
      const stripe = window.Stripe(publishableKey)
      const stripeResult = await stripe.confirmCardPayment(latestPayment.nextAction.clientSecret)

      if (stripeResult.error) {
        throw new Error(stripeResult.error.message || 'Stripe authentication failed')
      }

      setAuthMessage('Authentication completed. Finalizing payment...')
      latestPayment = await completeAuthenticationWithRetry()
    }

    if (shouldContinuePolling(latestPayment.status)) {
      latestPayment = await waitForStablePaymentState()
    } else {
      await refetch()
    }
    setAuthMessage(`Payment updated successfully. Status: ${formatStatus(latestPayment.status)}`)
  }

  const handleResumeAuthentication = async () => {
    try {
      await finalizeWithStripeIfNeeded()
    } catch (err: any) {
      const message = toPaymentFlowMessage(err)
      setAuthError(message)
      setAuthMessage(null)
      toast.error(message)
    }
  }

  const currentUrl = typeof window !== 'undefined' ? window.location.href : ''

  const handleCopyPaymentLink = async () => {
    try {
      await navigator.clipboard.writeText(currentUrl)
      toast.success('Payment link copied')
    } catch {
      toast.error('Unable to copy payment link')
    }
  }

  const handleDownloadReceipt = () => {
    const receiptLines = [
      'Payment Receipt',
      `Payment ID: ${payment.id}`,
      `Status: ${formatStatus(payment.status)}`,
      `Amount: ${formatCurrency(payment.amount, payment.currency)}`,
      `Customer: ${payment.customer?.email || 'N/A'}`,
      `Created: ${formatDate(payment.createdAt)}`,
      payment.capturedAt ? `Captured: ${formatDate(payment.capturedAt)}` : null,
      payment.failureReason ? `Failure: ${payment.failureReason}` : null,
    ].filter(Boolean) as string[]

    const blob = new Blob([receiptLines.join('\n')], { type: 'text/plain;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = `payment-${payment.id.slice(0, 8)}-receipt.txt`
    document.body.appendChild(anchor)
    anchor.click()
    anchor.remove()
    URL.revokeObjectURL(url)
    toast.success('Receipt downloaded')
  }

  const handleSharePayment = async () => {
    if (!navigator.share) {
      await handleCopyPaymentLink()
      return
    }

    try {
      await navigator.share({
        title: `Payment ${payment.id.slice(0, 8)}`,
        text: `${formatStatus(payment.status)} • ${formatCurrency(payment.amount, payment.currency)}`,
        url: currentUrl,
      })
    } catch {
      // User may cancel share sheet; no toast needed.
    }
  }
  
  return (
    <div className="payment-detail">
      <section className="payment-detail__hero">
        <div>
          <p className="payment-detail__eyebrow">Payment Record</p>
          <h1>Payment Details</h1>
          <p className="payment-detail__subtitle">
            Review lifecycle state, customer details, risk signals, and operational actions for this payment.
          </p>
          <div className="payment-detail__hero-meta">
            <span className="payment-detail__chip">ID: {payment.id.substring(0, 8)}...</span>
            <span className={`payment-detail__status payment-detail__status--${payment.status}`}>
              {formatStatus(payment.status)}
            </span>
          </div>
        </div>
        <div className="payment-detail__hero-amount-card">
          <div className="payment-detail__hero-amount-label">Amount</div>
          <div className="payment-detail__hero-amount-value">{formatCurrency(payment.amount, payment.currency)}</div>
          <div className="payment-detail__hero-amount-note">
            Created {formatDate(payment.createdAt)}
            {payment.capturedAt ? ` • Captured ${formatDate(payment.capturedAt)}` : ''}
          </div>
        </div>
      </section>

      {(authMessage || authError) && (
        <div
          className={`payment-detail__auth-banner ${authError ? 'payment-detail__auth-banner--error' : ''}`}
          role={authError ? 'alert' : 'status'}
          aria-live={authError ? 'assertive' : 'polite'}
        >
          {authError ? `Authentication Error: ${authError}` : authMessage}
        </div>
      )}

      <section className="payment-detail__grid">
        <div className="payment-detail__main">
          <div className="payment-detail__card">
            <div className="payment-detail__card-header">
              <div>
                <p className="payment-detail__section-eyebrow">Overview</p>
                <h3>Basic Information</h3>
              </div>
            </div>

            <div className="payment-detail__kv-grid">
              <div className="payment-detail__kv">
                <span>Payment ID</span>
                <code>{payment.id}</code>
              </div>
              <div className="payment-detail__kv">
                <span>Status</span>
                <div>
                  <span className={`payment-detail__status payment-detail__status--${payment.status}`}>
                    {formatStatus(payment.status)}
                  </span>
                </div>
              </div>
              <div className="payment-detail__kv">
                <span>Amount</span>
                <strong>{formatCurrency(payment.amount, payment.currency)}</strong>
              </div>
              <div className="payment-detail__kv">
                <span>Created</span>
                <div>{formatDate(payment.createdAt)}</div>
              </div>
              {payment.authorizedAt && (
                <div className="payment-detail__kv">
                  <span>Authorized</span>
                  <div>{formatDate(payment.authorizedAt)}</div>
                </div>
              )}
              {payment.capturedAt && (
                <div className="payment-detail__kv">
                  <span>Captured</span>
                  <div>{formatDate(payment.capturedAt)}</div>
                </div>
              )}
              {(payment.failureCode || payment.failureReason) && (
                <div className="payment-detail__kv payment-detail__kv--full">
                  <span>Failure Details</span>
                  <div className="payment-detail__failure">
                    {payment.failureCode ? <code>{payment.failureCode}</code> : null}
                    {payment.failureReason ? <p>{payment.failureReason}</p> : null}
                  </div>
                </div>
              )}
            </div>
          </div>

          <div className="payment-detail__card">
            <div className="payment-detail__card-header">
              <div>
                <p className="payment-detail__section-eyebrow">Customer</p>
                <h3>Customer & Payment Method</h3>
              </div>
            </div>

            <div className="payment-detail__kv-grid">
              <div className="payment-detail__kv">
                <span>Name</span>
                <div>{payment.customer?.name || 'N/A'}</div>
              </div>
              <div className="payment-detail__kv">
                <span>Email</span>
                <div>{payment.customer?.email || 'N/A'}</div>
              </div>
              <div className="payment-detail__kv">
                <span>Customer ID</span>
                <code>{payment.customer?.id || 'N/A'}</code>
              </div>
              <div className="payment-detail__kv">
                <span>Payment Method</span>
                <div>{payment.paymentMethod?.type || 'N/A'}</div>
              </div>
              {payment.paymentMethod?.id && (
                <div className="payment-detail__kv payment-detail__kv--full">
                  <span>Processor Payment Method ID</span>
                  <code>{payment.paymentMethod.id}</code>
                </div>
              )}
            </div>
          </div>

          {payment.fraudDetails && (
            <div className="payment-detail__card">
              <div className="payment-detail__card-header">
                <div>
                  <p className="payment-detail__section-eyebrow">Risk</p>
                  <h3>Fraud Assessment</h3>
                </div>
              </div>

              <div className="payment-detail__fraud">
                <div className="payment-detail__fraud-score">
                  <span className="payment-detail__fraud-label">Fraud Score</span>
                  <strong>{payment.fraudDetails.score}</strong>
                  <small>/ 100</small>
                </div>
                <div className="payment-detail__fraud-risk">
                  <span>Risk Level</span>
                  <span className={`payment-detail__risk-pill payment-detail__risk-pill--${payment.fraudDetails.riskLevel}`}>
                    {payment.fraudDetails.riskLevel.toUpperCase()}
                  </span>
                </div>
              </div>
            </div>
          )}

          {payment.refunds && payment.refunds.length > 0 && (
            <div className="payment-detail__card">
              <div className="payment-detail__card-header">
                <div>
                  <p className="payment-detail__section-eyebrow">Refund Activity</p>
                  <h3>Refunds</h3>
                </div>
                <span className="payment-detail__count-chip">{payment.refunds.length} refunds</span>
              </div>

              <div className="payment-detail__table-wrap">
                <table className="payment-detail__table">
                  <thead>
                    <tr>
                      <th>Refund ID</th>
                      <th>Amount</th>
                      <th>Status</th>
                      <th>Reason</th>
                      <th>Created</th>
                    </tr>
                  </thead>
                  <tbody>
                    {payment.refunds.map((refund) => (
                      <tr key={refund.id}>
                        <td><code>{refund.id.substring(0, 8)}...</code></td>
                        <td className="payment-detail__table-amount">{formatCurrency(refund.amount, refund.currency)}</td>
                        <td>
                          <span className={`payment-detail__refund-status payment-detail__refund-status--${refund.status}`}>
                            {formatStatus(refund.status)}
                          </span>
                        </td>
                        <td>{refund.reason || 'N/A'}</td>
                        <td>{formatDate(refund.createdAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>

        <aside className="payment-detail__side">
          <div className="payment-detail__card payment-detail__card--sticky">
            <div className="payment-detail__card-header">
              <div>
                <p className="payment-detail__section-eyebrow">Actions</p>
                <h3>Payment Operations</h3>
              </div>
            </div>

            <div className="payment-detail__actions">
              {payment.status === 'authorized' && (
                <button
                  onClick={() => setShowCaptureConfirm(true)}
                  disabled={capturePayment.isPending}
                  className="payment-detail__btn payment-detail__btn--success"
                  type="button"
                >
                  {capturePayment.isPending ? 'Capturing...' : 'Capture Payment'}
                </button>
              )}

              {payment.status === 'pending' && (
                <div className="payment-detail__action-block">
                  <button
                    onClick={handleResumeAuthentication}
                    disabled={completeAuth.isPending}
                    className="payment-detail__btn payment-detail__btn--primary"
                    type="button"
                  >
                    {completeAuth.isPending ? 'Resuming...' : 'Resume Authentication'}
                  </button>
                  <p className="payment-detail__action-help">
                    Use this if the payment is pending after Stripe authentication or page refresh.
                  </p>
                </div>
              )}

              {(payment.status === 'captured' || payment.status === 'partially_refunded') && (
                <div className="payment-detail__refund-form">
                  <div className="payment-detail__refund-title">Create Refund</div>

                  <label className="payment-detail__field">
                    <span>Refund Amount ({payment.currency})</span>
                    <input
                      type="number"
                      step="0.01"
                      value={refundAmount}
                      onChange={(e) => setRefundAmount(e.target.value)}
                      placeholder={`Leave empty for full refund • Max ${(payment.amount / 100).toFixed(2)}`}
                    />
                  </label>

                  <label className="payment-detail__field">
                    <span>Reason</span>
                    <select value={refundReason} onChange={(e) => setRefundReason(e.target.value)}>
                      <option value="customer_request">Customer Request</option>
                      <option value="duplicate">Duplicate</option>
                      <option value="fraudulent">Fraudulent</option>
                      <option value="other">Other</option>
                    </select>
                  </label>

                  <button
                    onClick={() => setShowRefundConfirm(true)}
                    disabled={refundPayment.isPending}
                    className="payment-detail__btn payment-detail__btn--warning"
                    type="button"
                  >
                    {refundPayment.isPending ? 'Processing...' : 'Create Refund'}
                  </button>
                </div>
              )}

              {payment.status !== 'authorized' &&
                payment.status !== 'pending' &&
                payment.status !== 'captured' &&
                payment.status !== 'partially_refunded' && (
                  <div className="payment-detail__empty-actions">
                    No direct actions are available for the current payment state.
                  </div>
                )}

              <div className="payment-detail__action-block">
                <div className="payment-detail__refund-title">Receipt & Share</div>
                <button
                  onClick={handleDownloadReceipt}
                  className="payment-detail__btn payment-detail__btn--secondary"
                  type="button"
                >
                  Download Receipt
                </button>
                <button
                  onClick={handleCopyPaymentLink}
                  className="payment-detail__btn payment-detail__btn--secondary"
                  type="button"
                >
                  Copy Payment Link
                </button>
                <button
                  onClick={handleSharePayment}
                  className="payment-detail__btn payment-detail__btn--secondary"
                  type="button"
                >
                  Share Payment
                </button>
              </div>
            </div>
          </div>
        </aside>
      </section>

      <ConfirmDialog
        open={showCaptureConfirm}
        title="Capture Payment"
        message={<p>Are you sure you want to capture this payment?</p>}
        confirmLabel="Capture Payment"
        tone="success"
        busy={capturePayment.isPending}
        onConfirm={handleCapture}
        onClose={() => setShowCaptureConfirm(false)}
      />

      <ConfirmDialog
        open={showRefundConfirm}
        title="Create Refund"
        message={
          <p>
            Refund{' '}
            <strong>
              {refundAmount ? formatCurrency(parseFloat(refundAmount) * 100, payment.currency) : 'the full amount'}
            </strong>
            ? This action will create a refund request for this payment.
          </p>
        }
        confirmLabel="Create Refund"
        tone="warning"
        busy={refundPayment.isPending}
        onConfirm={handleRefund}
        onClose={() => setShowRefundConfirm(false)}
      />
    </div>
  )
}
