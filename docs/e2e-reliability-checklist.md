# E2E Reliability Checklist

Use this before release tags and demo recordings.

## Preconditions

- All services are healthy in Eureka.
- Postgres, Redis, Kafka are up.
- Gateway routes are reachable.
- Test merchant account exists (or can be freshly registered).

## Flow Verification

1. Register + Login
   - Register a new merchant user.
   - Login succeeds and dashboard loads.
   - API keys page loads without backend fallback errors.

2. Create Payment
   - Create a payment from the dashboard.
   - Payment appears in All Payments with expected initial status.

3. Authentication + Capture
   - Complete auth / resume auth flow.
   - Payment transitions to `captured`.
   - Payment list reflects updated status after navigation/refresh.

4. Refund
   - Create refund from payment details.
   - Refund appears in refunds list and ties to payment/customer.
   - Duplicate refund attempt is blocked with clear error handling.

5. Webhook Sync
   - Forward Stripe events to backend webhook endpoint.
   - Verify duplicate webhook delivery handling is idempotent.
   - Verify state transitions are monotonic (no downgrade from terminal states).

6. Ticketing
   - Create a ticket linked to payment/refund context.
   - Update assignee/status/notes.
   - Timeline/thread reflects updates correctly.

7. Reliability Regression Checks
   - No stale status in UI after successful operations.
   - Retry/error states recover on subsequent successful API calls.
   - No unexpected `service unavailable` fallbacks when all dependencies are healthy.

## Evidence to Attach

- Screenshot or short clip for each major step.
- Logs for webhook processing (including duplicate webhook case).
- CI links for `ci.yml` and `integration-tests.yml` runs used for sign-off.
