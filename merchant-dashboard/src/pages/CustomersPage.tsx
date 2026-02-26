import CustomersList from '@/components/customers/CustomersList'
import './CustomersPage.css'

export default function CustomersPage() {
  return (
    <div className="customers-page">
      <section className="customers-page__hero">
        <div>
          <p className="customers-page__eyebrow">Customer Intelligence</p>
          <h1>Customers</h1>
          <p className="customers-page__subtitle">
            Review customer value, repeat purchase behavior, and individual payment history from a single workspace.
          </p>
        </div>
      </section>
      <CustomersList />
    </div>
  )
}
