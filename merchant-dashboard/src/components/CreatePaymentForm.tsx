import { useState } from 'react'
import { useCreatePayment } from '@/hooks/usePayments'
import type { CreatePaymentRequest } from '@/types/payment'

export default function CreatePaymentForm() {
  const [amount, setAmount] = useState('')
  const [currency, setCurrency] = useState('USD')
  const [email, setEmail] = useState('')
  const [name, setName] = useState('')
  const [cardToken] = useState('tok_visa_4242') // Mock token
  
  const createPayment = useCreatePayment()
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    const request: CreatePaymentRequest = {
      amount: Math.round(parseFloat(amount) * 100), // Convert to cents
      currency,
      paymentMethod: {
        type: 'card',
        cardToken,
      },
      customer: {
        email,
        name: name || undefined,
      },
      capture: true,
    }
    
    try {
      const payment = await createPayment.mutateAsync(request)
      alert(`Payment created! ID: ${payment.id}, Status: ${payment.status}`)
      
      // Reset form
      setAmount('')
      setEmail('')
      setName('')
    } catch (error: any) {
      alert(`Error: ${error.response?.data?.error?.message || error.message}`)
    }
  }
  
  return (
    <div style={{ maxWidth: '500px', margin: '20px' }}>
      <h2>Create Payment</h2>
      
      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: '15px' }}>
          <label>
            Amount ($):
            <input
              type="number"
              step="0.01"
              min="0.50"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              required
              style={{ width: '100%', padding: '8px', marginTop: '5px' }}
            />
          </label>
        </div>
        
        <div style={{ marginBottom: '15px' }}>
          <label>
            Currency:
            <select
              value={currency}
              onChange={(e) => setCurrency(e.target.value)}
              style={{ width: '100%', padding: '8px', marginTop: '5px' }}
            >
              <option value="USD">USD</option>
              <option value="EUR">EUR</option>
              <option value="GBP">GBP</option>
            </select>
          </label>
        </div>
        
        <div style={{ marginBottom: '15px' }}>
          <label>
            Customer Email:
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              style={{ width: '100%', padding: '8px', marginTop: '5px' }}
            />
          </label>
        </div>
        
        <div style={{ marginBottom: '15px' }}>
          <label>
            Customer Name (optional):
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              style={{ width: '100%', padding: '8px', marginTop: '5px' }}
            />
          </label>
        </div>
        
        <div style={{ marginBottom: '15px', padding: '10px', background: '#f0f0f0' }}>
          <small>
            Test Card: Visa ending in 4242 (auto-approve)
            <br />
            Card Token: {cardToken}
          </small>
        </div>
        
        <button
          type="submit"
          disabled={createPayment.isPending}
          style={{
            width: '100%',
            padding: '12px',
            background: createPayment.isPending ? '#ccc' : '#007bff',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: createPayment.isPending ? 'not-allowed' : 'pointer',
          }}
        >
          {createPayment.isPending ? 'Processing...' : 'Create Payment'}
        </button>
      </form>
      
      {createPayment.isError && (
        <div style={{ marginTop: '15px', padding: '10px', background: '#ffebee', color: 'red' }}>
          Error: {(createPayment.error as any)?.response?.data?.error?.message || 'Unknown error'}
        </div>
      )}
      
      {createPayment.isSuccess && (
        <div style={{ marginTop: '15px', padding: '10px', background: '#e8f5e9', color: 'green' }}>
          ✅ Payment created successfully! Status: {createPayment.data.status}
        </div>
      )}
    </div>
  )
}