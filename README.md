# fineract-hook-api


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



