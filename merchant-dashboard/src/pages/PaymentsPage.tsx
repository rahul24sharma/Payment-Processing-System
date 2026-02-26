import PaymentList from '@/components/PaymentList'
import './PaymentsPage.css'

export default function PaymentsPage() {
  return (
    <div className="payments-page">
      <section className="payments-page__hero">
        <div>
          <p className="payments-page__eyebrow">Payment Operations</p>
          <h1>All payments</h1>
          <p className="payments-page__subtitle">
            Monitor the full payment lifecycle, filter by status, and jump into any payment for capture, refunds, or SCA recovery.
          </p>
        </div>
      </section>
      <PaymentList />
    </div>
  )
}
