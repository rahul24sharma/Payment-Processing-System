import CreatePaymentForm from '@/components/CreatePaymentForm'
import './CreatePaymentPage.css'

export default function CreatePaymentPage() {
  return (
    <div className="create-payment-page">
      <section className="create-payment-page__hero">
        <div>
          <p className="create-payment-page__eyebrow">New Charge</p>
          <h1>Create a payment</h1>
          <p className="create-payment-page__subtitle">
            Collect a card payment with Stripe, support SCA authentication, and track the result in your payment timeline.
          </p>
        </div>
      </section>

      <div className="create-payment-page__layout">
        <aside className="create-payment-page__aside">
          <div className="cp-side-card">
            <h3>Checklist</h3>
            <ul>
              <li>Use customer email and legal name</li>
              <li>Provide full billing address (required for India export rules)</li>
              <li>Use a Stripe test card in dev mode</li>
              <li>Pending payments can be resumed from Payment Detail</li>
            </ul>
          </div>

          <div className="cp-side-card cp-side-card--muted">
            <h3>Test Cards</h3>
            <p><strong>4242 4242 4242 4242</strong> for standard success.</p>
            <p>Use Stripe’s 3DS test cards to validate authentication-required flows.</p>
          </div>
        </aside>

        <section className="create-payment-page__main">
          <CreatePaymentForm />
        </section>
      </div>
    </div>
  )
}
