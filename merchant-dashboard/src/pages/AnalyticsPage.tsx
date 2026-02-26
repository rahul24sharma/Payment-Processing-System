import { useState } from 'react'
import StatsCards from '@/components/analytics/StatsCards'
import RevenueChart from '@/components/analytics/RevenueChart'
import PaymentStatusChart from '@/components/analytics/PaymentStatusChart'
import FraudScoreChart from '@/components/analytics/FraudScoreChart'
import PaymentFunnelChart from '@/components/analytics/PaymentFunnelChart'
import TopCustomersTable from '@/components/analytics/TopCustomersTable'
import { TIME_RANGES } from '@/types/analytics'
import './AnalyticsPage.css'

export default function AnalyticsPage() {
  const [selectedRange, setSelectedRange] = useState(30) // Default: Last 30 days

  const selectedRangeLabel = TIME_RANGES.find((r) => r.days === selectedRange)?.label || 'Last 30 days'

  return (
    <div className="analytics-page">
      <section className="analytics-page__hero">
        <div className="analytics-page__hero-copy">
          <p className="analytics-page__eyebrow">Performance Intelligence</p>
          <h1>Analytics Control Room</h1>
          <p className="analytics-page__subtitle">
            Monitor payment performance, conversion quality, fraud patterns, and top customer behavior in one visual
            command center.
          </p>
          <div className="analytics-page__hero-tags">
            <span className="analytics-page__chip analytics-page__chip--accent">{selectedRangeLabel}</span>
            <span className="analytics-page__chip">Live from payments data</span>
            <span className="analytics-page__chip">Ops + finance ready</span>
          </div>
        </div>
        <div className="analytics-page__hero-controls">
          <label className="analytics-page__range-field">
            <span>Time Range</span>
            <select value={selectedRange} onChange={(e) => setSelectedRange(Number(e.target.value))}>
              {TIME_RANGES.map((range) => (
                <option key={range.days} value={range.days}>
                  {range.label}
                </option>
              ))}
            </select>
          </label>
          <div className="analytics-page__spotlight">
            <div className="analytics-page__spotlight-label">Snapshot</div>
            <div className="analytics-page__spotlight-value">{selectedRange}d</div>
            <div className="analytics-page__spotlight-note">Rolling analytics window</div>
          </div>
        </div>
      </section>

      <StatsCards days={selectedRange} />

      <section className="analytics-page__chart-grid">
        <div className="analytics-page__chart-card analytics-page__chart-card--wide">
          <RevenueChart days={selectedRange} />
        </div>
        <div className="analytics-page__chart-card">
          <PaymentStatusChart days={selectedRange} />
        </div>
        <div className="analytics-page__chart-card">
          <PaymentFunnelChart days={selectedRange} />
        </div>
        <div className="analytics-page__chart-card">
          <FraudScoreChart days={selectedRange} />
        </div>
      </section>

      <section className="analytics-page__bottom-grid">
        <div className="analytics-page__panel analytics-page__panel--table">
          <TopCustomersTable days={selectedRange} />
        </div>

        <div className="analytics-page__panel analytics-page__panel--export">
          <div className="analytics-page__panel-header">
            <p className="analytics-page__eyebrow">Exports</p>
            <h3>Reporting Outputs</h3>
            <p>
              Download finance-ready snapshots for audits, board reports, or deeper analysis in your BI tool of
              choice.
            </p>
          </div>

          <div className="analytics-page__export-stack">
            <button type="button" className="analytics-page__export-btn analytics-page__export-btn--success">
              <span>CSV Export</span>
              <small>Transactions, statuses, customers</small>
            </button>

            <button type="button" className="analytics-page__export-btn analytics-page__export-btn--primary">
              <span>PDF Summary</span>
              <small>Executive report layout</small>
            </button>

            <div className="analytics-page__export-note">
              <strong>Next:</strong> Scheduled reports and S3 delivery can be wired after analytics backend endpoints are
              finalized.
            </div>
          </div>
        </div>
      </section>
    </div>
  )
}
