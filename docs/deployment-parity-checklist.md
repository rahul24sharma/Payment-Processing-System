# Deployment Parity Checklist (Dev / Stage / Prod)

## Config Parity

- Same service list enabled in all environments.
- Same gateway routes/filters (rate limiter + circuit breaker) with env-specific URIs only.
- Same database schema version and migration order.
- Same Kafka topic names/partitions per environment conventions.

## Secret Parity

- Secrets are env-only (no plaintext in YAML/source).
- `JWT_SECRET` set and rotated by environment.
- `STRIPE_SECRET_KEY` and `STRIPE_WEBHOOK_SECRET` set per environment.
- Merchant encryption keys configured:
  - `MERCHANT_BANK_ACCOUNT_ENCRYPTION_KEYS`
  - `MERCHANT_BANK_ACCOUNT_ENCRYPTION_ACTIVE_KEY_ID`

## Observability Parity

- Prometheus scraping all services in each environment.
- Alert rules loaded (`monitoring/prometheus/alerts.yml`).
- Dashboard panels for:
  - service availability
  - gateway 5xx
  - login failures
  - webhook failures
  - Kafka lag

## RBAC / Audit Parity

- Backend role checks active on admin/developer endpoints.
- Frontend route guards aligned with backend role policy.
- Audit logs recorded for sensitive actions:
  - API key create/revoke
  - merchant profile/settings updates
  - ticket status changes
  - maintenance/re-encryption actions

## Release Validation

1. Run smoke tests for register/login/payment/refund/ticket/webhook.
2. Verify no fallback “service unavailable” on successful backend responses.
3. Verify pending -> captured/refunded status sync in UI (auto-refresh + manual refresh).
4. Verify alerting hooks by simulating one controlled failure per class.

