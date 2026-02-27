# Operations Runbook

## Scope

This runbook covers operational debugging for:

- service down incidents
- webhook delivery failures
- login/auth failures
- Kafka delivery lag/replay workflows

## 1) Service Down

1. Check local process status:
   - `./stop-local.sh`
   - `./start-local.sh`
2. Check health endpoints:
   - `http://localhost:8080/actuator/health` (gateway)
   - `http://localhost:8081/actuator/health` (payment)
   - `http://localhost:8086/actuator/health` (merchant)
3. Check logs:
   - `.local-run/logs/api-gateway.log`
   - `.local-run/logs/payment-service.log`
   - `.local-run/logs/merchant-service.log`
4. Validate dependencies:
   - Postgres `5432`
   - Redis `6379`
   - Kafka `29092` or `9092`
   - Eureka `8761`

## 2) Webhook Failures (Stripe -> payment-service)

1. Verify webhook endpoint:
   - `POST /api/v1/webhooks/stripe`
2. Verify secret:
   - `STRIPE_WEBHOOK_SECRET` is set and current.
3. Replay events:
   - `stripe listen --forward-to http://localhost:8081/api/v1/webhooks/stripe`
   - `stripe events resend <event_id>`
4. Check failure patterns:
   - signature verification errors
   - duplicate events (idempotency path)
   - missing payment mapping (`processor_payment_id`)

## 3) Auth Failures

1. Check `JWT_SECRET` exists and is at least 32 chars.
2. Re-login and confirm `merchant_role` is present in browser local storage.
3. Validate gateway route to merchant auth:
   - `/api/v1/auth/**` forwards to merchant-service.
4. Check recent 401s in merchant-service logs.

## 4) Kafka Lag / Event Replay

1. Confirm broker is reachable (`localhost:29092` or `localhost:9092`).
2. Check consumer lag via dashboard/metrics.
3. For duplicate deliveries, verify consumer idempotency keys are active.
4. Replay strategy:
   - replay from topic with filtered payment IDs
   - run in staging first
   - validate no duplicate side effects in notification/ledger tables

## 5) Refund/Payment State Drift

1. Compare Stripe payment intent state with internal payment row.
2. Trigger reconciliation by hitting payment detail refresh in UI.
3. If still stale, replay webhook event for that intent and verify status transition logs.

