# Payment Processing System

Microservices-based payment platform with Spring Boot backend services and a React merchant dashboard.

[![CI](https://github.com/yourusername/Payment-Processing-System/actions/workflows/ci.yml/badge.svg)](https://github.com/yourusername/Payment-Processing-System/actions/workflows/ci.yml)
[![Integration Tests](https://github.com/yourusername/Payment-Processing-System/actions/workflows/integration-tests.yml/badge.svg)](https://github.com/yourusername/Payment-Processing-System/actions/workflows/integration-tests.yml)

## Architecture

```text
Merchant Dashboard (React/Vite)
           |
           v
      API Gateway (8080)
           |
  -----------------------------
  |      |      |      |      |
Payment Fraud Ledger Settlement Merchant Notification
(8081) (8082) (8083)   (8084)   (8086)    (8085)
           |
           v
      Kafka Event Bus
```

## Service Boundaries

- `api-gateway`: routing, request filtering, centralized entrypoint.
- `payment-service`: payment lifecycle, Stripe integration, webhook status reconciliation.
- `fraud-service`: fraud scoring and risk events.
- `ledger-service`: accounting records for financial state transitions.
- `settlement-service`: settlement/payout orchestration.
- `merchant-service`: merchant auth, profile, API keys, settings.
- `notification-service`: outbound notifications and webhook deliveries.
- `merchant-dashboard`: merchant-facing UI for payments, refunds, tickets, webhooks, API keys.

## Local Setup

### Prerequisites

- Java 21
- Maven 3.9+
- Node 20+
- PostgreSQL (port `5432`)
- Redis (port `6379`)
- Kafka (port `9092` or `29092`)

### 1) Configure env

- Copy `.env.example` to `.env` and fill required secrets.
- Use a JWT secret of at least 32 bytes.

### 2) Start services

```bash
./start-local.sh
```

### 3) Start dashboard

```bash
cd merchant-dashboard
npm ci
npm run dev
```

## CI Workflows

- `ci.yml`: per-service build and test matrix (unit/service tests).
- `integration-tests.yml`: infra-backed suite with Postgres + Redis + Kafka.

## End-to-End Reliability Flow (Manual)

Run this clean flow before releases:

1. Register merchant and login.
2. Create payment (`pending`/`requires_auth`).
3. Complete authentication.
4. Capture payment (`captured`).
5. Create refund and verify refund status/history.
6. Trigger Stripe webhook events and verify backend state sync.
7. Create/update ticket and verify timeline/status updates.

Reference checklist: `docs/e2e-reliability-checklist.md`.

## Demo Flow Assets

Add screenshots/GIFs in `docs/demo/` and reference them here:

- `docs/demo/dashboard.png`
- `docs/demo/create-payment.png`
- `docs/demo/payment-lifecycle.gif`
- `docs/demo/refund-flow.png`
- `docs/demo/webhook-events.png`

## Security & Compliance Status

### Implemented

- JWT auth with role support and audit logging hooks.
- API key management.
- Stripe webhook signature verification.
- Idempotency controls in payment/event flows.
- Field-level encryption for sensitive merchant bank account fields.
- Encryption key versioning + re-encryption endpoint support.

### Not Production-Complete Yet

- Managed KMS/HSM-backed key custody and rotation automation.
- Full SOC2/PCI evidence program (controls, audit artifacts, policy lifecycle).
- Full incident alerting/runbook automation across all services.
- Comprehensive chaos/load/resilience certification under production traffic models.

See `docs/security-compliance-pass.md` for details.

## Notes

This project is production-oriented in architecture, but still requires operational hardening and compliance evidence work before claiming enterprise production readiness.
