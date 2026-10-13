# fineract-hook-api

Receives Fineract webhooks and publishes them to Kafka. Notification topics and the curl calls used to register each hook are in [docs/FINERACT_HOOKS.md](docs/FINERACT_HOOKS.md).

## CI/CD (GitHub Actions)

- **Build and push image** (`.github/workflows/build-push-image.yml`): on push to `main`, builds and pushes the image to Docker Hub as `nexusbank/fineract-hook-api:latest` and `nexusbank/fineract-hook-api:<short-sha>`.
- **Release image** (`.github/workflows/release-image.yml`): when you create a GitHub Release or push a tag `v*` (e.g. `v1.0.0`), builds and pushes `nexusbank/fineract-hook-api:<tag>` (e.g. `nexusbank/fineract-hook-api:v1.0.0`).

**Required repo secrets:** `DOCKER_USER`, `DOCKER_PASSWORD`.  
**Optional repo variable:** `DOCKER_ORGANIZATION` (default: `nexusbank`).

## Local build / push

```bash
docker build --platform linux/amd64,linux/arm64 -t nexusbank/fineract-hook-api .
docker tag nexusbank/fineract-hook-api  nexusbank/fineract-hook-api:latest    
docker login
docker push nexusbank/fineract-hook-api:latest

docker buildx build --network=host --platform linux/amd64,linux/arm64   -t nexusbank/fineract-hook-api:latest  --push .
```

Create fineract hook
https://localhost:8443/fineract-provider/api/v1/hooks
{
"name": "Web",
"isActive": true,
"displayName": "All Savings Account Transactions Hook",
//"templateId": 1,
"events": [
{ "actionName": "DEPOSIT", "entityName": "SAVINGSACCOUNT" },
{ "actionName": "WITHDRAW", "entityName": "SAVINGSACCOUNT" },
{ "actionName": "WITHDRAWAL", "entityName": "SAVINGSACCOUNT" },
{ "actionName": "ADJUSTTRANSACTION", "entityName": "SAVINGSACCOUNT" },
{ "actionName": "UNDOTRANSACTION", "entityName": "SAVINGSACCOUNT" },
{ "actionName": "REVERSETRANSACTION", "entityName": "SAVINGSACCOUNT" },
{ "actionName": "REFUNDBYTRANSFER", "entityName": "SAVINGSACCOUNT" },
{ "actionName": "POSTINTEREST", "entityName": "SAVINGSACCOUNT" }
],
"config": {
"Payload URL": "http://localhost:9999/hooks/transactions/",
"Content Type": "json"
}
}

{
"name": "Web",
"isActive": true,
"displayName": "Kremlin",
"events": [
{
"actionName": "DISBURSE",
"entityName": "LOAN"
},
{
"actionName": "REPAYMENT",
"entityName": "LOAN"
}
],
"config": {
"Payload URL": "http://example.com/webhook/",
"Content Type": "json"
}
}

_{
"name": "web",
"isActive": true,
"displayName": "AllSavings Account Transactions Hook",
//"templateId": "1",
"events": [
{ "actionName": "DEPOSIT", "entityName": "SAVINGSACCOUNT" },
{ "actionName": "WITHDRAW", "entityName": "SAVINGSACCOUNT" },
{ "actionName": "WITHDRAWAL", "entityName": "SAVINGSACCOUNT" },
{ "actionName": "ADJUSTTRANSACTION", "entityName": "SAVINGSACCOUNT" },
{ "actionName": "UNDOTRANSACTION", "entityName": "SAVINGSACCOUNT" },
{ "actionName": "REVERSETRANSACTION", "entityName": "SAVINGSACCOUNT" },
{ "actionName": "REFUNDBYTRANSFER", "entityName": "SAVINGSACCOUNT" },
{ "actionName": "POSTINTEREST", "entityName": "SAVINGSACCOUNT" }
],
"config": {
"Payload URL": "https://hooks-api.sandbox.nexus-bank.io/hooks/transactions/",
"Content Type": "json"
}
}_


