package com.nexusbank.corebanking.hook.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusbank.corebanking.hook.model.Transaction;
import com.nexusbank.corebanking.hook.model.TransactionEvent;
import com.nexusbank.corebanking.hook.model.TransactionPayload;
import com.nexusbank.corebanking.hook.model.TransactionRequest;
import com.nexusbank.corebanking.hook.model.TransactionResponse;
import com.nexusbank.corebanking.hook.services.KafkaProducerService;
import com.nexusbank.corebanking.hook.services.NotificationDestination;
import com.nexusbank.corebanking.hook.services.NotificationTopicRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hooks")
public class HookController {

    private static final Logger log = LoggerFactory.getLogger(HookController.class);
    private final KafkaProducerService kafkaProducerService;
    private final NotificationTopicRouter topicRouter;
    private final ObjectMapper objectMapper;

    @Value("${fineract.tenantId}")
    private String tenantId;

    public HookController(
            KafkaProducerService kafkaProducerService,
            NotificationTopicRouter topicRouter,
            ObjectMapper objectMapper) {
        this.kafkaProducerService = kafkaProducerService;
        this.topicRouter = topicRouter;
        this.objectMapper = objectMapper;
    }

    /**
     * Existing savings-transaction hook. Keeps the legacy transactions topic and also
     * publishes the event on the notification group topic selected from the Fineract action.
     */
    @PostMapping({"/transactions/", "/transactions", "/events/", "/events"})
    public ResponseEntity<String> receiveHook(@RequestBody Transaction transaction) {
        logPayload(transaction);
        TransactionEvent event = toEvent(transaction, null);
        kafkaProducerService.sendEvent(event);
        NotificationDestination destination = topicRouter.route(transaction.getEntityName(), transaction.getActionName());
        event.setNotificationType(destination.notificationType());
        kafkaProducerService.send(destination.topic(), event);
        return ResponseEntity.ok("Hook received and sent to Kafka topic " + destination.topic());
    }

    /**
     * Explicit destination. Register one Fineract hook per notification family and point
     * Payload URL at /hooks/notifications/{type}.
     */
    @PostMapping({"/notifications/{type}", "/notifications/{type}/"})
    public ResponseEntity<String> receiveTypedHook(@PathVariable String type, @RequestBody Transaction transaction) {
        logPayload(transaction);
        final String topic;
        try {
            topic = topicRouter.topicForType(type);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
        TransactionEvent event = toEvent(transaction, topicRouter.route(transaction.getEntityName(), transaction.getActionName()).notificationType());
        if ("transaction".equalsIgnoreCase(type)) {
            event.setNotificationType("TRANSACTION");
        } else if ("payment-confirmation".equalsIgnoreCase(type) || "payment_confirmation".equalsIgnoreCase(type)) {
            event.setNotificationType("PAYMENT_CONFIRMATION");
        } else if ("account".equalsIgnoreCase(type)) {
            event.setNotificationType("ACCOUNT");
        } else if ("card".equalsIgnoreCase(type)) {
            event.setNotificationType("CARD");
        }
        kafkaProducerService.send(topic, event);
        return ResponseEntity.ok("Hook received and sent to Kafka topic " + topic);
    }

    private TransactionEvent toEvent(Transaction transaction, String notificationType) {
        TransactionRequest request = transaction.getRequest();
        TransactionResponse response = transaction.getResponse();
        TransactionPayload payload = new TransactionPayload();
        if (request != null) {
            payload.setDate(request.getTransactionDate());
            payload.setNote(request.getNote());
            payload.setAmount(String.valueOf(request.getTransactionAmount()));
        }
        if (response != null) {
            payload.setTransactionId(String.valueOf(response.getResourceId()));
            payload.setClientId(String.valueOf(response.getClientId()));
            payload.setAccountId(String.valueOf(response.getSavingsId()));
        } else if (transaction.getClientId() != 0) {
            payload.setClientId(String.valueOf(transaction.getClientId()));
        }
        payload.setTransactionType(transaction.getActionName());
        TransactionEvent event = new TransactionEvent();
        event.setPayload(payload);
        event.setAction("CREATION");
        event.setEntity(transaction.getEntityName());
        event.setResourceId(response == null ? null : String.valueOf(response.getResourceId()));
        event.setTenantId(tenantId);
        event.setNotificationType(notificationType);
        return event;
    }

    private void logPayload(Transaction transaction) {
        try {
            log.info(objectMapper.writeValueAsString(transaction));
        } catch (JsonProcessingException e) {
            log.warn("Unable to log Fineract hook payload");
        }
    }
}
