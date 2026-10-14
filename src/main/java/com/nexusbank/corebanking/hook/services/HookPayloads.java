package com.nexusbank.corebanking.hook.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusbank.corebanking.hook.model.TransactionEvent;
import com.nexusbank.corebanking.hook.model.TransactionPayload;

/**
 * Turns a Fineract hook body into the Kafka payload notification-api consumes.
 * Savings commands use request.transactionAmount and response.clientId.
 * Interop transfers use request.accountId (IBAN) and request.amount.amount.
 */
public final class HookPayloads {

    private HookPayloads() {
    }

    public static TransactionEvent toEvent(
            JsonNode body,
            String tenantId,
            String notificationType,
            SavingsClientLookup lookup) {
        JsonNode request = body == null ? null : body.get("request");
        JsonNode response = body == null ? null : body.get("response");
        String accountId = first(
                text(request, "accountId"),
                text(response, "savingsId"),
                text(body, "accountId"));
        String clientId = nonzero(first(text(response, "clientId"), text(body, "clientId")));
        String amount = text(request, "transactionAmount");
        String currency = null;
        JsonNode amountNode = request == null ? null : request.get("amount");
        if (amountNode != null && amountNode.isObject()) {
            amount = text(amountNode, "amount");
            currency = text(amountNode, "currency");
        } else if (amountNode != null && (amountNode.isNumber() || amountNode.isTextual())) {
            amount = amountNode.asText();
        }
        if (clientId == null && lookup != null && accountId != null) {
            SavingsClientLookup.ClientAccount account = lookup.findByIdentifier(accountId);
            if (account != null) {
                clientId = nonzero(account.clientId());
                if (account.savingsId() != null && !account.savingsId().isBlank()) {
                    accountId = account.savingsId();
                }
            }
        }
        TransactionPayload payload = new TransactionPayload();
        payload.setAccountId(accountId);
        payload.setClientId(clientId);
        payload.setAmount(amount);
        payload.setCurrency(currency);
        payload.setDate(text(request, "transactionDate"));
        payload.setNote(text(request, "note"));
        payload.setTransactionType(text(body, "actionName"));
        payload.setTransactionRole(text(request, "transactionRole"));
        payload.setTransactionId(first(
                text(response, "resourceId"),
                text(response, "transferCode"),
                text(request, "transferCode")));
        payload.setStatus(text(response, "state"));

        TransactionEvent event = new TransactionEvent();
        event.setPayload(payload);
        event.setAction("CREATION");
        event.setEntity(text(body, "entityName"));
        event.setResourceId(payload.getTransactionId());
        event.setTenantId(tenantId);
        event.setNotificationType(notificationType);
        return event;
    }

    private static String first(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value)) {
                return value;
            }
        }
        return null;
    }

    private static String nonzero(String value) {
        if (value == null || value.isBlank() || "0".equals(value) || "null".equalsIgnoreCase(value)) {
            return null;
        }
        return value;
    }

    private static String text(JsonNode node, String field) {
        if (node == null || field == null || node.get(field) == null || node.get(field).isNull()) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value.isTextual() || value.isNumber() || value.isBoolean()) {
            String text = value.asText();
            return text == null || text.isBlank() ? null : text;
        }
        return null;
    }
}
