import { useState } from 'react'
import { CardElement, Elements, useElements, useStripe } from '@stripe/react-stripe-js'
import { loadStripe } from '@stripe/stripe-js'
import type { StripeCardElementChangeEvent } from '@stripe/stripe-js'
import { useCreatePayment } from '@/hooks/usePayments'
import { paymentsApi } from '@/api/payments'
import { useToast } from '@/contexts/ToastContext'
import type { CreatePaymentRequest, Payment } from '@/types/payment'
import './CreatePaymentForm.css'

const publishableKey = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY as string | undefined
const stripePromise = publishableKey ? loadStripe(publishableKey) : null

function CreatePaymentFormInner() {
  const stripe = useStripe()
  const elements = useElements()
  const [amount, setAmount] = useState('')
  const [currency, setCurrency] = useState('USD')
  const [email, setEmail] = useState('')
  const [name, setName] = useState('')
  const [addressLine1, setAddressLine1] = useState('')
  const [addressLine2, setAddressLine2] = useState('')
  const [city, setCity] = useState('')
  const [state, setState] = useState('')
  const [postalCode, setPostalCode] = useState('')
  const [country, setCountry] = useState('IN')
  const [scaMessage, setScaMessage] = useState<string | null>(null)
  const [lastPaymentStatus, setLastPaymentStatus] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [isFinalizingAuth, setIsFinalizingAuth] = useState(false)
  const [cardReady, setCardReady] = useState(false)
  const secureContext = typeof window === 'undefined' ? true : window.isSecureContext
  
  const createPayment = useCreatePayment()
  const toast = useToast()

  const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

  const shouldContinuePolling = (status?: string) =>
    status === 'pending' || status === 'authorized'

  const waitForPaymentSettlement = async (paymentId: string, seed?: Payment) => {
    let current = seed
    const startedAt = Date.now()
    const timeoutMs = 20_000

    while (Date.now() - startedAt < timeoutMs) {
      if (current && !shouldContinuePolling(current.status)) {
        return current
      }
      await sleep(2000)
      current = await paymentsApi.get(paymentId)
      setScaMessage(`Waiting for final payment status... current: ${current.status}`)
    }
    return current ?? paymentsApi.get(paymentId)
  }

  const isPaymentServiceUnavailableError = (error: any) => {
    const data = error?.response?.data
    return (
      data?.service === 'payment-service' &&
      typeof data?.message === 'string' &&
      data.message.includes('currently unavailable')
    )
  }

  const toPaymentFlowMessage = (error: any) => {
    const raw =
      error?.response?.data?.error?.message ||
      error?.response?.data?.message ||
      error?.message ||
      'Unknown error'

    const lower = String(raw).toLowerCase()
    if (lower.includes('authentication failed')) return 'Authentication was not completed. Please try again.'
    if (lower.includes('authentication canceled') || lower.includes('canceled')) return 'Authentication was canceled. You can retry from the payment detail page.'
    if (lower.includes('currently unavailable')) return 'Payment service is temporarily unavailable. Please retry in a few seconds.'
    if (lower.includes('timed out') || lower.includes('timeout')) return 'The request timed out while finalizing payment. Check the payment list before retrying.'
    if (lower.includes('card was declined') || lower.includes('declined')) return raw
    return raw
  }

  const completeAuthenticationWithRetry = async (paymentId: string) => {
    const delays = [0, 1200, 2500, 4000]
    let lastError: any

    for (let attempt = 0; attempt < delays.length; attempt += 1) {
      if (delays[attempt] > 0) {
        setScaMessage(`Payment service reconnecting... retrying (${attempt + 1}/${delays.length})`)
        await sleep(delays[attempt])
      }

      try {
        setScaMessage('Authentication completed. Finalizing payment with backend...')
        return await paymentsApi.completeAuthentication(paymentId)
      } catch (error: any) {
        lastError = error
        if (!isPaymentServiceUnavailableError(error) || attempt === delays.length - 1) {
          throw error
        }
      }
    }

    throw lastError
  }
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    try {
      if (!stripe || !elements) {
        throw new Error('Stripe card form is not ready. Refresh and try again.')
      }
      const cardElement = elements.getElement(CardElement)
      if (!cardElement) {
        throw new Error('Card input is unavailable. Refresh and try again.')
      }

      const paymentMethodResult = await stripe.createPaymentMethod({
        type: 'card',
        card: cardElement,
        billing_details: {
          name: name || undefined,
          email: email || undefined,
          address: {
            line1: addressLine1 || undefined,
            line2: addressLine2 || undefined,
            city: city || undefined,
            state: state || undefined,
            postal_code: postalCode || undefined,
            country: country || undefined,
          },
        },
      })

      if (paymentMethodResult.error || !paymentMethodResult.paymentMethod?.id) {
        throw new Error(paymentMethodResult.error?.message || 'Failed to create Stripe payment method')
      }

      const request: CreatePaymentRequest = {
        amount: Math.round(parseFloat(amount) * 100), // Convert to cents
        currency,
        paymentMethod: {
          type: 'card',
          savedPaymentMethodId: paymentMethodResult.paymentMethod.id,
        },
        customer: {
          email,
          name: name || undefined,
          address: {
            line1: addressLine1 || undefined,
            line2: addressLine2 || undefined,
            city: city || undefined,
            state: state || undefined,
            postalCode: postalCode || undefined,
            country: country || undefined,
          },
        },
        capture: true,
      }

      setFormError(null)
      setScaMessage(null)
      setLastPaymentStatus(null)
      setIsFinalizingAuth(false)
      let payment = await createPayment.mutateAsync(request)

      if (payment.nextAction?.type === 'use_stripe_sdk' && payment.nextAction.clientSecret) {
        setScaMessage('Additional authentication required. Opening Stripe authentication...')

        if (!stripe) {
          throw new Error(
            'Stripe is not initialized. Refresh and try again.'
          )
        }

        const stripeResult = await stripe.confirmCardPayment(payment.nextAction.clientSecret)

        if (stripeResult.error) {
          throw new Error(stripeResult.error.message || 'Stripe authentication failed')
        }

        setIsFinalizingAuth(true)
        payment = await completeAuthenticationWithRetry(payment.id)
        setIsFinalizingAuth(false)
      }

      if (shouldContinuePolling(payment.status)) {
        setScaMessage(`Waiting for final payment status... current: ${payment.status}`)
        payment = await waitForPaymentSettlement(payment.id, payment)
      }

      toast.success(`Payment created! ID: ${payment.id}, Status: ${payment.status}`)
      setScaMessage(null)
      setLastPaymentStatus(payment.status)
      
      // Reset full form state after successful payment
      setAmount('')
      setCurrency('USD')
      setEmail('')
      setName('')
      setAddressLine1('')
      setAddressLine2('')
      setCity('')
      setState('')
      setPostalCode('')
      setCountry('IN')
      setFormError(null)

      // Clear Stripe card input so card number/expiry/cvc are wiped.
      cardElement.clear()
    } catch (error: any) {
      setIsFinalizingAuth(false)
      setScaMessage(null)
      setLastPaymentStatus(null)
      const message = toPaymentFlowMessage(error)
      setFormError(message || 'Unknown error')
      toast.error(message || 'Unknown error')
    }
  }
  
  return (
    <div className="cpf">
      <div className="cpf__card">
        <div className="cpf__header">
          <div>
            <p className="cpf__eyebrow">Payment Request</p>
            <h2>Create payment intent</h2>
          </div>
          <span className="cpf__badge">{stripe && elements && cardReady ? 'Stripe Ready' : 'Initializing Stripe...'}</span>
        </div>

        <form className="cpf__form" onSubmit={handleSubmit}>
          <section className="cpf__section">
            <h3>Amount</h3>
            <div className="cpf__grid cpf__grid--2">
              <label className="cpf__field">
                <span>Amount</span>
                <input
                  type="number"
                  step="0.01"
                  min="0.50"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  required
                  placeholder="100.00"
                />
              </label>

              <label className="cpf__field">
                <span>Currency</span>
                <select
                  value={currency}
                  onChange={(e) => setCurrency(e.target.value)}
                >
                  <option value="USD">USD</option>
                  <option value="INR">INR</option>
                  <option value="EUR">EUR</option>
                  <option value="GBP">GBP</option>
                </select>
              </label>
            </div>
          </section>

          <section className="cpf__section">
            <h3>Customer</h3>
            <div className="cpf__grid cpf__grid--2">
              <label className="cpf__field">
                <span>Email</span>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  placeholder="customer@example.com"
                />
              </label>

              <label className="cpf__field">
                <span>Full name</span>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                  placeholder="Aarav Sharma"
                />
              </label>
            </div>
          </section>

          <section className="cpf__section">
            <h3>Billing Address</h3>
            <div className="cpf__grid cpf__grid--2">
              <label className="cpf__field cpf__field--full">
                <span>Address line 1</span>
                <input
                  type="text"
                  value={addressLine1}
                  onChange={(e) => setAddressLine1(e.target.value)}
                  required
                />
              </label>

              <label className="cpf__field cpf__field--full">
                <span>Address line 2 <em>(optional)</em></span>
                <input
                  type="text"
                  value={addressLine2}
                  onChange={(e) => setAddressLine2(e.target.value)}
                />
              </label>

              <label className="cpf__field">
                <span>City</span>
                <input
                  type="text"
                  value={city}
                  onChange={(e) => setCity(e.target.value)}
                  required
                />
              </label>

              <label className="cpf__field">
                <span>State</span>
                <input
                  type="text"
                  value={state}
                  onChange={(e) => setState(e.target.value)}
                  required
                />
              </label>

              <label className="cpf__field">
                <span>Postal code</span>
                <input
                  type="text"
                  value={postalCode}
                  onChange={(e) => setPostalCode(e.target.value)}
                  required
                />
              </label>

              <label className="cpf__field">
                <span>Country (ISO2)</span>
                <input
                  type="text"
                  value={country}
                  onChange={(e) => setCountry(e.target.value.toUpperCase())}
                  required
                  minLength={2}
                  maxLength={2}
                  style={{ textTransform: 'uppercase' }}
                />
              </label>
            </div>
          </section>

          <section className="cpf__section">
            <h3>Card Details</h3>
            {!secureContext && (
              <div className="cpf__alert cpf__alert--info" role="status" aria-live="polite">
                Secure context is required for best Stripe card entry behavior. Open the app on <strong>https://localhost:5173</strong>.
              </div>
            )}
            <div className="cpf__field">
              <span id="stripe-card-element-label">Stripe Card Element</span>
              <div className="cpf__card-element" role="group" aria-labelledby="stripe-card-element-label">
                <CardElement
                  options={{
                    hidePostalCode: true,
                    style: {
                      base: {
                        fontSize: '16px',
                        color: '#0f172a',
                        '::placeholder': {
                          color: '#94a3b8',
                        },
                      },
                      invalid: {
                        color: '#b42318',
                      },
                    },
                  }}
                  onReady={() => setCardReady(true)}
                  onChange={(event: StripeCardElementChangeEvent) => {
                    if (event.error?.message) {
                      setFormError(event.error.message)
                    } else if (formError && formError.toLowerCase().includes('card')) {
                      setFormError(null)
                    }
                  }}
                />
              </div>
            </div>
            <p className="cpf__hint">
              Use a Stripe test card like <strong>4242 4242 4242 4242</strong>. SCA test cards can be used to verify auth flows.
            </p>
          </section>

          <div className="cpf__actions">
            <button
              className="cpf__submit"
              type="submit"
              disabled={createPayment.isPending || isFinalizingAuth || !stripe || !elements || !cardReady}
            >
              {createPayment.isPending || isFinalizingAuth
                ? 'Processing...'
                : !stripe || !elements || !cardReady
                  ? 'Loading Card Form...'
                  : 'Create Payment'}
            </button>
          </div>
        </form>
      </div>

      {formError && <div className="cpf__alert cpf__alert--error" role="alert" aria-live="assertive">Error: {formError}</div>}
      {scaMessage && <div className="cpf__alert cpf__alert--info" role="status" aria-live="polite">{scaMessage}</div>}
      {createPayment.isSuccess && lastPaymentStatus && (
        <div className="cpf__alert cpf__alert--success" role="status" aria-live="polite">
          Payment created successfully. Status: {lastPaymentStatus}
        </div>
      )}
    </div>
  )
}

export default function CreatePaymentForm() {
  if (!publishableKey || !stripePromise) {
    return (
      <div className="cpf__alert cpf__alert--error" role="alert" aria-live="assertive">
        Stripe is not configured. Set <code>VITE_STRIPE_PUBLISHABLE_KEY</code> and restart the frontend.
      </div>
    )
  }

  return (
    <Elements stripe={stripePromise}>
      <CreatePaymentFormInner />
    </Elements>
  )
}
