package com.nexusbank.corebanking.hook.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class NotificationTopicRouter {

  private static final Set<String> ACCOUNT_ACTIONS = Set.of(
      "CREATE", "APPROVE", "ACTIVATE", "REJECT", "CLOSE", "BLOCK", "UNBLOCK", "CLOSEASRESCHEDULED");

  private final String transactionTopic;
  private final String paymentConfirmationTopic;
  private final String accountTopic;
  private final String cardTopic;

  public NotificationTopicRouter(
      @Value("${fineract.kafka.topic.notification-transaction}") String transactionTopic,
      @Value("${fineract.kafka.topic.notification-payment-confirmation}") String paymentConfirmationTopic,
      @Value("${fineract.kafka.topic.notification-account}") String accountTopic,
      @Value("${fineract.kafka.topic.notification-card}") String cardTopic) {
    this.transactionTopic = transactionTopic;
    this.paymentConfirmationTopic = paymentConfirmationTopic;
    this.accountTopic = accountTopic;
    this.cardTopic = cardTopic;
  }

  public NotificationDestination route(String entityName, String actionName) {
    String entity = upper(entityName);
    String action = upper(actionName);
    if (isPaymentConfirmation(entity, action)) {
      return new NotificationDestination("PAYMENT_CONFIRMATION", paymentConfirmationTopic);
    }
    if (entity.contains("CARD")) {
      return new NotificationDestination("CARD", cardTopic);
    }
    if (isAccountLifecycle(entity, action)) {
      return new NotificationDestination("ACCOUNT", accountTopic);
    }
    return new NotificationDestination("TRANSACTION", transactionTopic);
  }

  public String topicForType(String type) {
    String normalized = upper(type).replace('-', '_');
    return switch (normalized) {
      case "PAYMENT_CONFIRMATION", "PAYMENTCONFIRMATION" -> paymentConfirmationTopic;
      case "ACCOUNT" -> accountTopic;
      case "CARD" -> cardTopic;
      case "TRANSACTION" -> transactionTopic;
      default -> throw new IllegalArgumentException("Unsupported notification type: " + type);
    };
  }

  static boolean isPaymentConfirmation(String entity, String action) {
    return action.contains("CONFIRM")
        || action.contains("AUTHORIZE")
        || entity.contains("ONLINEPAYMENT")
        || "PAYMENT".equals(entity);
  }

  static boolean isAccountLifecycle(String entity, String action) {
    if ("CLIENT".equals(entity) || "GROUP".equals(entity) || "OFFICE".equals(entity)) {
      return true;
    }
    return ("SAVINGSACCOUNT".equals(entity) || "LOAN".equals(entity)) && ACCOUNT_ACTIONS.contains(action);
  }

  private static String upper(String value) {
    return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
  }
}
