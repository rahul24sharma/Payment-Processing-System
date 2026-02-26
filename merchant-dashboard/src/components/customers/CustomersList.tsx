import { Link } from 'react-router-dom'
import { usePayments } from '@/hooks/usePayments'
import AsyncState from '@/components/ui/AsyncState'
import { formatCurrency, formatDate, formatStatus } from '@/utils/formatters'
import type { Payment } from '@/types/payment'
import { useEffect, useId, useMemo, useRef, useState } from 'react'
import './CustomersList.css'

interface CustomerData {
  customerId: string
  email: string
  name?: string
  totalSpent: number
  paymentCount: number
  lastPaymentDate: string
  firstPaymentDate: string
}

export default function CustomersList() {
  const { data, isLoading, error } = usePayments()
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedCustomer, setSelectedCustomer] = useState<CustomerData | null>(null)

  const customers = useMemo(() => {
    if (!data?.data) return []

    const customerMap = new Map<string, CustomerData>()

    data.data.forEach((payment: Payment) => {
      if (!payment.customer) return

      const customerId = payment.customer.id
      const existing = customerMap.get(customerId)

      if (existing) {
        existing.totalSpent += payment.amount
        existing.paymentCount += 1
        if (new Date(payment.createdAt) > new Date(existing.lastPaymentDate)) {
          existing.lastPaymentDate = payment.createdAt
        }
        if (new Date(payment.createdAt) < new Date(existing.firstPaymentDate)) {
          existing.firstPaymentDate = payment.createdAt
        }
      } else {
        customerMap.set(customerId, {
          customerId,
          email: payment.customer.email,
          name: payment.customer.name,
          totalSpent: payment.amount,
          paymentCount: 1,
          lastPaymentDate: payment.createdAt,
          firstPaymentDate: payment.createdAt,
        })
      }
    })

    return Array.from(customerMap.values()).sort((a, b) => b.totalSpent - a.totalSpent)
  }, [data])

  const filteredCustomers = useMemo(() => {
    if (!searchQuery) return customers
    const query = searchQuery.toLowerCase()
    return customers.filter(
      (c) => c.email.toLowerCase().includes(query) || c.name?.toLowerCase().includes(query)
    )
  }, [customers, searchQuery])

  const paymentsForSelected =
    selectedCustomer
      ? data?.data.filter((p: Payment) => p.customer?.id === selectedCustomer.customerId) || []
      : []

  const repeatCustomers = customers.filter((c) => c.paymentCount > 1).length
  const avgCustomerValue =
    customers.reduce((sum, c) => sum + c.totalSpent, 0) / (customers.length || 1)

  if (isLoading) {
    return (
      <AsyncState
        kind="loading"
        title="Loading customers"
        message="Building your customer directory from payment history."
      />
    )
  }

  if (error) {
    return <AsyncState kind="error" title="Unable to load customers" message={error.message} />
  }

  if (customers.length === 0) {
    return (
      <AsyncState
        kind="empty"
        title="No customers yet"
        message="Customer records will appear here once payments are processed."
      />
    )
  }

  return (
    <div className="customers-list">
      <div className="customers-list__stats">
        <StatCard label="Total Customers" value={String(customers.length)} tone="neutral" />
        <StatCard label="Avg. Customer Value" value={formatCurrency(avgCustomerValue)} tone="success" />
        <StatCard label="Repeat Customers" value={String(repeatCustomers)} tone="info" />
      </div>

      <div className="customers-list__panel">
        <div className="customers-list__toolbar">
          <div>
            <p className="customers-list__eyebrow">Search & Review</p>
            <h2>Customer directory</h2>
          </div>
          <span className="customers-list__toolbar-meta">{filteredCustomers.length} shown</span>
        </div>

        <div className="customers-list__filters">
          <label className="customers-list__field">
            <span>Search customers</span>
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search by email or name"
            />
          </label>
        </div>

        <div className="customers-list__table-wrap">
          <table className="customers-list__table">
            <thead>
              <tr>
                <th>Customer</th>
                <th>Total Spent</th>
                <th>Payments</th>
                <th>First Payment</th>
                <th>Last Payment</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {filteredCustomers.map((customer) => (
                <tr key={customer.customerId}>
                  <td>
                    <div className="customers-list__customer-cell">
                      <span className="customers-list__customer-name">{customer.name || 'Anonymous'}</span>
                      <span className="customers-list__subtle">{customer.email}</span>
                      <span className="customers-list__subtle">{customer.customerId}</span>
                    </div>
                  </td>
                  <td className="customers-list__money">{formatCurrency(customer.totalSpent)}</td>
                  <td>
                    <div className="customers-list__payments-count">
                      <span>{customer.paymentCount}</span>
                      {customer.paymentCount > 1 && (
                        <span className="customers-list__repeat">Repeat</span>
                      )}
                    </div>
                  </td>
                  <td>{formatDate(customer.firstPaymentDate)}</td>
                  <td>{formatDate(customer.lastPaymentDate)}</td>
                  <td>
                    <button
                      type="button"
                      className="customers-list__view-btn"
                      onClick={() => setSelectedCustomer(customer)}
                    >
                      View Details
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {filteredCustomers.length === 0 && (
          <AsyncState
            kind="empty"
            compact
            title="No customers match your search"
            message={`Try a different name or email search than "${searchQuery}".`}
          />
        )}
      </div>

      {selectedCustomer && (
        <CustomerDetailModal
          customer={selectedCustomer}
          payments={paymentsForSelected}
          onClose={() => setSelectedCustomer(null)}
        />
      )}
    </div>
  )
}

function StatCard({
  label,
  value,
  tone,
}: {
  label: string
  value: string
  tone: 'neutral' | 'success' | 'info'
}) {
  return (
    <div className={`customers-list__stat-card customers-list__stat-card--${tone}`}>
      <div className="customers-list__stat-label">{label}</div>
      <div className="customers-list__stat-value">{value}</div>
    </div>
  )
}

function CustomerDetailModal({
  customer,
  payments,
  onClose,
}: {
  customer: CustomerData
  payments: Payment[]
  onClose: () => void
}) {
  const titleId = useId()
  const panelRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    panelRef.current?.focus()

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.body.style.overflow = previousOverflow
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [onClose])

  return (
    <div className="customer-modal" role="dialog" aria-modal="true" aria-labelledby={titleId}>
      <button type="button" className="customer-modal__backdrop" onClick={onClose} aria-label="Close modal" />

      <div className="customer-modal__panel" ref={panelRef} tabIndex={-1}>
        <div className="customer-modal__header">
          <div>
            <p className="customer-modal__eyebrow">Customer Details</p>
            <h2 id={titleId}>{customer.name || 'Anonymous Customer'}</h2>
            <p className="customer-modal__subhead">{customer.email}</p>
          </div>
          <button type="button" className="customer-modal__close" onClick={onClose} aria-label="Close">
            ×
          </button>
        </div>

        <div className="customer-modal__stats">
          <div className="customer-modal__stat">
            <span>Total Spent</span>
            <strong>{formatCurrency(customer.totalSpent)}</strong>
          </div>
          <div className="customer-modal__stat">
            <span>Payments</span>
            <strong>{customer.paymentCount}</strong>
          </div>
          <div className="customer-modal__stat">
            <span>First Payment</span>
            <strong>{formatDate(customer.firstPaymentDate)}</strong>
          </div>
          <div className="customer-modal__stat">
            <span>Last Payment</span>
            <strong>{formatDate(customer.lastPaymentDate)}</strong>
          </div>
        </div>

        <div className="customer-modal__history">
          <div className="customer-modal__history-header">
            <h3>Payment History</h3>
            <span>{payments.length} payments</span>
          </div>

          <div className="customer-modal__table-wrap">
            <table className="customer-modal__table">
              <thead>
                <tr>
                  <th>Payment</th>
                  <th>Amount</th>
                  <th>Status</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {payments.map((payment) => (
                  <tr key={payment.id}>
                    <td>
                      <Link to={`/payments/${payment.id}`} className="customer-modal__payment-link" onClick={onClose}>
                        {payment.id.substring(0, 8)}...
                      </Link>
                    </td>
                    <td className="customer-modal__amount">{formatCurrency(payment.amount, payment.currency)}</td>
                    <td>
                      <span className={`customer-modal__status customer-modal__status--${payment.status}`}>
                        {formatStatus(payment.status)}
                      </span>
                    </td>
                    <td>{formatDate(payment.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <button type="button" className="customer-modal__footer-close" onClick={onClose}>
          Close
        </button>
      </div>
    </div>
  )
}
