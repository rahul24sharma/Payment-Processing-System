import RefundsList from '@/components/refunds/RefundsList'
import './RefundsPage.css'

export default function RefundsPage() {
  return (
    <div className="refunds-page">
      <section className="refunds-page__hero">
        <div>
          <p className="refunds-page__eyebrow">Post-Payment Operations</p>
          <h1>Refunds</h1>
          <p className="refunds-page__subtitle">
            Track all refund attempts, monitor failed refunds, and jump back to the source payment when investigation is needed.
          </p>
        </div>
      </section>

      <RefundsList />
    </div>
  )
}
