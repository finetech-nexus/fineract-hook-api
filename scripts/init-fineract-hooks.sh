#!/usr/bin/env bash
# Register the Fineract web hooks that publish into the notification topics.
# Skips a hook when its display name is already present.
#
#   FINERACT_URL  Fineract base URL (no trailing path)
#   TENANT        Fineract-Platform-TenantId
#   AUTH          user:password
#   HOOKS_URL     fineract-hook-api base URL reachable from the Fineract pod
#
# Example:
#   FINERACT_URL=https://fynams.dev.sandbox.internal.nbank.fr \
#   TENANT=nexusbank AUTH='mifos:password' \
#   HOOKS_URL=https://hooks-api.dev.sandbox.internal.nbank.fr \
#   ./scripts/init-fineract-hooks.sh

set -euo pipefail

FINERACT_URL="${FINERACT_URL:-https://fynams.dev.sandbox.internal.nbank.fr}"
TENANT="${TENANT:-nexusbank}"
AUTH="${AUTH:-mifos:password}"
HOOKS_URL="${HOOKS_URL:-https://hooks-api.dev.sandbox.internal.nbank.fr}"
FINERACT_URL="${FINERACT_URL%/}"
HOOKS_URL="${HOOKS_URL%/}"

existing="$(curl -fsS -u "$AUTH" -H "Fineract-Platform-TenantId: $TENANT" \
  "$FINERACT_URL/fineract-provider/api/v1/hooks")"

create_hook() {
  local display="$1"
  local path="$2"
  local events="$3"
  if printf '%s' "$existing" | grep -F "\"$display\"" >/dev/null; then
    echo "already registered: $display"
    return 0
  fi
  echo "registering: $display -> $HOOKS_URL$path"
  curl -fsS -X POST "$FINERACT_URL/fineract-provider/api/v1/hooks" \
    -u "$AUTH" \
    -H "Fineract-Platform-TenantId: $TENANT" \
    -H "Content-Type: application/json" \
    -d "{
      \"name\": \"Web\",
      \"isActive\": true,
      \"displayName\": \"$display\",
      \"events\": $events,
      \"config\": {
        \"Payload URL\": \"$HOOKS_URL$path\",
        \"Content Type\": \"json\"
      }
    }"
  echo
}

create_hook "Notification savings transactions" "/hooks/notifications/transaction" '[
  {"actionName":"DEPOSIT","entityName":"SAVINGSACCOUNT"},
  {"actionName":"WITHDRAWAL","entityName":"SAVINGSACCOUNT"},
  {"actionName":"WITHDRAW","entityName":"SAVINGSACCOUNT"},
  {"actionName":"ADJUSTTRANSACTION","entityName":"SAVINGSACCOUNT"},
  {"actionName":"UNDOTRANSACTION","entityName":"SAVINGSACCOUNT"},
  {"actionName":"REVERSETRANSACTION","entityName":"SAVINGSACCOUNT"},
  {"actionName":"REFUNDBYTRANSFER","entityName":"SAVINGSACCOUNT"},
  {"actionName":"POSTINTEREST","entityName":"SAVINGSACCOUNT"}
]'

create_hook "Notification loan transactions" "/hooks/notifications/transaction" '[
  {"actionName":"DISBURSE","entityName":"LOAN"},
  {"actionName":"REPAYMENT","entityName":"LOAN"},
  {"actionName":"REFUNDBYTRANSFER","entityName":"LOAN"}
]'

create_hook "Notification online payment confirmation" "/hooks/notifications/payment-confirmation" '[
  {"actionName":"CONFIRM","entityName":"ONLINEPAYMENT"},
  {"actionName":"AUTHORIZE","entityName":"PAYMENT"}
]'

create_hook "Notification account lifecycle" "/hooks/notifications/account" '[
  {"actionName":"CREATE","entityName":"CLIENT"},
  {"actionName":"ACTIVATE","entityName":"CLIENT"},
  {"actionName":"REJECT","entityName":"CLIENT"},
  {"actionName":"APPROVE","entityName":"SAVINGSACCOUNT"},
  {"actionName":"ACTIVATE","entityName":"SAVINGSACCOUNT"},
  {"actionName":"CLOSE","entityName":"SAVINGSACCOUNT"},
  {"actionName":"BLOCK","entityName":"SAVINGSACCOUNT"}
]'

create_hook "Notification card lifecycle" "/hooks/notifications/card" '[
  {"actionName":"CREATE","entityName":"CARD"},
  {"actionName":"BLOCK","entityName":"CARD"},
  {"actionName":"UPDATE","entityName":"CARD"}
]'

echo "done"
