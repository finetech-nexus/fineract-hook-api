# Register Fineract hooks

Fineract calls `fineract-hook-api` after each business event. The hook API publishes that event to Kafka. `notification-api` consumes the notification topics and delivers the message on the channel configured in Notification Studio.

Base URL in dev:

- Tunisia: `https://hooks-api.dev.sandbox.internal.nbank.fr`
- Europe: `https://hooks-api.dev1.sandbox.internal.nbank.fr`

Fineract API (adjust host, tenant and credentials):

```bash
export FINERACT_URL=https://fynams.dev.sandbox.internal.nbank.fr
export TENANT=nexusbank
export AUTH="mifos:password"
```

Hooks use the built-in **Web** template. `Payload URL` and `Content Type` are the exact config keys Fineract expects.

## Topics

| Hook path | Kafka topic | When to use it |
| --- | --- | --- |
| `POST /hooks/transactions/` | legacy `transactions` **and** the routed notification topic | Already used by the savings-account hook. Keep it if another consumer still reads `transactions`. |
| `POST /hooks/notifications/transaction` | `notification.transaction` | Deposit, withdrawal, card payment, loan repayment, transfer received. |
| `POST /hooks/notifications/payment-confirmation` | `notification.payment-confirmation` | Online payment waiting for the customer to confirm. |
| `POST /hooks/notifications/account` | `notification.account` | Client or account opened, approved, rejected, blocked, closed. |
| `POST /hooks/notifications/card` | `notification.card` | Card blocked, limit reached, card shipped. |

`/hooks/events/` uses the same routing as `/hooks/transactions/` (legacy topic plus the matching notification topic).

Offers, security alerts and reminders are not Fineract events. Publish them on `notification.offer`, `notification.security` and `notification.reminder`. The message shape is the same `TransactionEvent` JSON.

Automatic routing on `/hooks/transactions/` and `/hooks/events/`:

- action contains `CONFIRM` or `AUTHORIZE`, or entity is `PAYMENT` / `ONLINEPAYMENT` → payment confirmation
- entity contains `CARD` → card
- entity `CLIENT`, `GROUP`, `OFFICE`, or a savings/loan lifecycle action (`CREATE`, `APPROVE`, `ACTIVATE`, `REJECT`, `CLOSE`, `BLOCK`, `UNBLOCK`) → account
- everything else (deposit, withdrawal, repayment, disbursement) → transaction

Do not register both `/hooks/transactions/` and `/hooks/notifications/transaction` for the same Fineract events. That would deliver the notification twice.

## Savings account movements

```bash
curl -X POST "$FINERACT_URL/fineract-provider/api/v1/hooks" \
  -u "$AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Web",
    "isActive": true,
    "displayName": "Notification savings transactions",
    "events": [
      { "actionName": "DEPOSIT", "entityName": "SAVINGSACCOUNT" },
      { "actionName": "WITHDRAWAL", "entityName": "SAVINGSACCOUNT" },
      { "actionName": "WITHDRAW", "entityName": "SAVINGSACCOUNT" },
      { "actionName": "ADJUSTTRANSACTION", "entityName": "SAVINGSACCOUNT" },
      { "actionName": "UNDOTRANSACTION", "entityName": "SAVINGSACCOUNT" },
      { "actionName": "REVERSETRANSACTION", "entityName": "SAVINGSACCOUNT" },
      { "actionName": "REFUNDBYTRANSFER", "entityName": "SAVINGSACCOUNT" },
      { "actionName": "POSTINTEREST", "entityName": "SAVINGSACCOUNT" }
    ],
    "config": {
      "Payload URL": "https://hooks-api.dev.sandbox.internal.nbank.fr/hooks/notifications/transaction",
      "Content Type": "json"
    }
  }'
```

## Loan movements

```bash
curl -X POST "$FINERACT_URL/fineract-provider/api/v1/hooks" \
  -u "$AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Web",
    "isActive": true,
    "displayName": "Notification loan transactions",
    "events": [
      { "actionName": "DISBURSE", "entityName": "LOAN" },
      { "actionName": "REPAYMENT", "entityName": "LOAN" },
      { "actionName": "REFUNDBYTRANSFER", "entityName": "LOAN" }
    ],
    "config": {
      "Payload URL": "https://hooks-api.dev.sandbox.internal.nbank.fr/hooks/notifications/transaction",
      "Content Type": "json"
    }
  }'
```

## Online payment confirmation

Point this hook at the Fineract actions that mean "the customer must confirm". If those actions are not in the template list, add them in Fineract first (`GET /fineract-provider/api/v1/hooks/template`), then register:

```bash
curl -X POST "$FINERACT_URL/fineract-provider/api/v1/hooks" \
  -u "$AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Web",
    "isActive": true,
    "displayName": "Notification online payment confirmation",
    "events": [
      { "actionName": "CONFIRM", "entityName": "ONLINEPAYMENT" },
      { "actionName": "AUTHORIZE", "entityName": "PAYMENT" }
    ],
    "config": {
      "Payload URL": "https://hooks-api.dev.sandbox.internal.nbank.fr/hooks/notifications/payment-confirmation",
      "Content Type": "json"
    }
  }'
```

## Account lifecycle

```bash
curl -X POST "$FINERACT_URL/fineract-provider/api/v1/hooks" \
  -u "$AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Web",
    "isActive": true,
    "displayName": "Notification account lifecycle",
    "events": [
      { "actionName": "CREATE", "entityName": "CLIENT" },
      { "actionName": "ACTIVATE", "entityName": "CLIENT" },
      { "actionName": "REJECT", "entityName": "CLIENT" },
      { "actionName": "APPROVE", "entityName": "SAVINGSACCOUNT" },
      { "actionName": "ACTIVATE", "entityName": "SAVINGSACCOUNT" },
      { "actionName": "CLOSE", "entityName": "SAVINGSACCOUNT" },
      { "actionName": "BLOCK", "entityName": "SAVINGSACCOUNT" }
    ],
    "config": {
      "Payload URL": "https://hooks-api.dev.sandbox.internal.nbank.fr/hooks/notifications/account",
      "Content Type": "json"
    }
  }'
```

## List, update, delete

```bash
curl -s -u "$AUTH" -H "Fineract-Platform-TenantId: $TENANT" \
  "$FINERACT_URL/fineract-provider/api/v1/hooks"

curl -X PUT "$FINERACT_URL/fineract-provider/api/v1/hooks/HOOK_ID" \
  -u "$AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Web",
    "isActive": false,
    "displayName": "Notification savings transactions",
    "events": [
      { "actionName": "DEPOSIT", "entityName": "SAVINGSACCOUNT" }
    ],
    "config": {
      "Payload URL": "https://hooks-api.dev.sandbox.internal.nbank.fr/hooks/notifications/transaction",
      "Content Type": "json"
    }
  }'

curl -X DELETE "$FINERACT_URL/fineract-provider/api/v1/hooks/HOOK_ID" \
  -u "$AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT"
```

Replace `HOOK_ID` with the id returned at creation. The hook payload URL must be reachable from the Fineract pod (`hooks-api` service DNS inside the cluster is preferable to the public host when both run in the same cluster).
