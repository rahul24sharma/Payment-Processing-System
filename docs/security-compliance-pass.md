# Security / Compliance Pass (Frontend + Backend)

## Scope

This pass covers:

- secrets/config hygiene (`application.yml` and env usage)
- Stripe webhook secret rotation process
- sensitive data handling review for merchant bank account settings

## Changes Applied

### 1. Secrets / Config Hygiene (Applied)

Hardcoded secrets/defaults were removed from local Spring configs and replaced with env-backed values:

- `POSTGRES_PASSWORD`
- `STRIPE_SECRET_KEY`
- `STRIPE_WEBHOOK_SECRET`
- `JWT_SECRET`
- `MERCHANT_BANK_ACCOUNT_ENCRYPTION_KEY` (base64-encoded 32-byte key, merchant payout data encryption)
- `MERCHANT_BANK_ACCOUNT_ENCRYPTION_KEYS` (optional keyring for rotation, `keyId:base64Key,...`)
- `MERCHANT_BANK_ACCOUNT_ENCRYPTION_ACTIVE_KEY_ID` (active key id for new encryptions)
- `MERCHANT_ADMIN_MAINTENANCE_TOKEN` (required to run maintenance re-encryption endpoint)

Updated files:

- `payment-service/src/main/resources/application.yml`
- `merchant-service/src/main/resources/application.yml`
- `notification-service/src/main/resources/application.yml`
- `settlement-service/src/main/resources/application.yml`
- `fraud-service/src/main/resources/application.yml`
- `ledger-service/src/main/resources/application.yml`
- `payment-service/src/test/resources/application-test.yml`

### Env-only policy (recommended)

- Never commit real `sk_*`, `whsec_*`, DB passwords, JWT secrets into YAML or source files.
- Keep secrets in:
  - local `.env` (ignored by git)
  - shell environment
  - CI/CD secret manager
  - runtime secret manager (Vault / AWS Secrets Manager / GCP Secret Manager)
- Config files may reference env vars only.

## 2. Stripe Webhook Secret Rotation Process

### Rotation triggers

- Suspected secret exposure
- Team member offboarding
- Scheduled rotation (e.g. every 90 days)
- Environment recreation (staging/prod)

### Rotation steps

1. Create a new Stripe webhook secret
   - Stripe Dashboard (or Stripe CLI for local dev)
   - For local dev:
     - `stripe listen --forward-to http://localhost:8081/api/v1/webhooks/stripe`
     - copy the printed `whsec_...`

2. Update runtime secret store / environment
   - set `STRIPE_WEBHOOK_SECRET=<new_whsec>`

3. Restart `payment-service`
   - verify webhook signature validation succeeds

4. Validate delivery
   - trigger an event or replay a recent event
   - confirm `payment-service` logs show successful webhook processing

5. Revoke old secret
   - remove old secret from secret store and local env files

### Operational notes

- Keep `whsec_*` out of logs and screenshots.
- Store dev/staging/prod webhook secrets separately.

## 3. Merchant Bank Account Encryption Key Rotation Process

The merchant-service bank account crypto now supports key IDs and versioned ciphertext (`enc:v2:<keyId>:...`).
This allows encrypting with an active key while still decrypting older values using prior keys.

### Rotation steps

1. Generate a new 32-byte key (base64)
   - `openssl rand -base64 32`

2. Add the new key to the keyring without removing old keys
   - `MERCHANT_BANK_ACCOUNT_ENCRYPTION_KEYS=v1:<oldBase64>,v2:<newBase64>`

3. Switch the active encryption key
   - `MERCHANT_BANK_ACCOUNT_ENCRYPTION_ACTIVE_KEY_ID=v2`

4. Restart `merchant-service`

5. Verify new payout account updates use the new key ID
   - New values should persist using the `enc:v2:v2:...` format

6. Re-encrypt old records (recommended)
   - Run a migration or controlled re-save process for existing bank account values

7. Remove the old key only after all old records are re-encrypted

### Admin-safe re-encryption endpoint

`merchant-service` now exposes a maintenance endpoint for batch migration:

- `POST /api/v1/admin/maintenance/reencrypt-bank-accounts`
- Header: `X-Admin-Maintenance-Token: <MERCHANT_ADMIN_MAINTENANCE_TOKEN>`

Query params:

- `dryRun` (default `true`)
- `maxMerchants` (default `500`)
- `pageSize` (default `100`)

Examples:

```bash
# Preview impacted records
curl -X POST "http://localhost:8086/api/v1/admin/maintenance/reencrypt-bank-accounts?dryRun=true&maxMerchants=200" \
  -H "X-Admin-Maintenance-Token: $MERCHANT_ADMIN_MAINTENANCE_TOKEN"

# Execute migration
curl -X POST "http://localhost:8086/api/v1/admin/maintenance/reencrypt-bank-accounts?dryRun=false&maxMerchants=200" \
  -H "X-Admin-Maintenance-Token: $MERCHANT_ADMIN_MAINTENANCE_TOKEN"
```

### Backward compatibility

- Existing legacy encrypted values (`enc:v1:<iv>:<ciphertext>`) continue to decrypt using:
  - `MERCHANT_BANK_ACCOUNT_ENCRYPTION_KEY`, or
  - the `v1` key in `MERCHANT_BANK_ACCOUNT_ENCRYPTION_KEYS`
- Plaintext historical values remain readable (for migration), but new writes require encryption

## 4. Sensitive Data Handling Review (Bank Account in `merchant-service`)

### Current state (after this pass)

- Bank account data is stored in `Merchant.settings` JSONB under `bankAccount`
- `accountNumber` and `routingNumber` are encrypted before persistence in `merchant-service` (AES-GCM)
- `accountNumberLast4` and `routingNumberLast4` are stored separately for UI display
- Backend responses now mask sensitive fields before returning to the frontend:
  - account number
  - routing number
- `settings` payload is also sanitized to avoid leaking raw bank account values
- Frontend no longer rehydrates sensitive bank numbers into the form automatically

### Remaining production risk

Bank account values are now encrypted at the application layer, but key management and rotation are still operator-managed via environment configuration. This is better than plaintext, but production-grade compliance should still move to managed KMS-backed encryption or tokenization.

### Production recommendations (next step)

1. Field-level encryption at rest (minimum)
   - Encrypt `accountNumber` and `routingNumber` before persistence
   - Use a managed KMS-backed key (AWS KMS / GCP KMS / Vault Transit)

2. Prefer tokenization (best)
   - Store a token/reference instead of raw bank account numbers
   - Use a payout processor / vault service for sensitive banking details

3. Data minimization
   - Store only what is required for settlement operations
   - Persist/display last4 separately for UI

4. Audit / access controls
   - Restrict which roles/services can update payout details
   - Add audit trail for payout account changes (who/when)

5. Logging controls
   - Never log raw bank account or routing values

## Implemented vs Remaining

### Implemented in this repo

1. Env-backed secret configuration across services (no required plaintext secrets in YAML defaults)
2. Stripe webhook secret rotation runbook
3. Bank account field encryption + masking
4. Encryption key versioning support and active key selection
5. Admin-safe re-encryption endpoint for migration

### Remaining for production-grade posture

1. Managed KMS/HSM integration for encryption keys (instead of env-managed key custody)
2. End-to-end audit trails for payout changes and other sensitive admin actions
3. Formal RBAC verification matrix across all admin endpoints
4. Secret rotation automation checks in CI/CD and runtime health checks
5. Compliance evidence package (SOC2/PCI controls, operational evidence, periodic reviews)
