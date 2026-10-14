package com.nexusbank.corebanking.hook.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusbank.corebanking.hook.model.TransactionEvent;
import com.nexusbank.corebanking.hook.services.HookPayloads;
import com.nexusbank.corebanking.hook.services.KafkaProducerService;
import com.nexusbank.corebanking.hook.services.NotificationDestination;
import com.nexusbank.corebanking.hook.services.NotificationTopicRouter;
import com.nexusbank.corebanking.hook.services.SavingsClientLookup;
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
    private final SavingsClientLookup clientLookup;

    @Value("${fineract.tenantId}")
    private String tenantId;

    public HookController(
            KafkaProducerService kafkaProducerService,
            NotificationTopicRouter topicRouter,
            SavingsClientLookup clientLookup) {
        this.kafkaProducerService = kafkaProducerService;
        this.topicRouter = topicRouter;
        this.clientLookup = clientLookup;
    }

    /**
     * Existing savings-transaction hook. Keeps the legacy transactions topic and also
     * publishes the event on the notification group topic selected from the Fineract action.
     */
    @PostMapping({"/transactions/", "/transactions", "/events/", "/events"})
    public ResponseEntity<String> receiveHook(@RequestBody JsonNode transaction) {
        logPayload(transaction);
        NotificationDestination destination = topicRouter.route(text(transaction, "entityName"), text(transaction, "actionName"));
        TransactionEvent event = HookPayloads.toEvent(transaction, tenantId, destination.notificationType(), clientLookup);
        kafkaProducerService.sendEvent(event);
        kafkaProducerService.send(destination.topic(), event);
        return ResponseEntity.ok("Hook received and sent to Kafka topic " + destination.topic());
    }

    /**
     * Explicit destination. Register one Fineract hook per notification family and point
     * Payload URL at /hooks/notifications/{type}.
     */
    @PostMapping({"/notifications/{type}", "/notifications/{type}/"})
    public ResponseEntity<String> receiveTypedHook(@PathVariable String type, @RequestBody JsonNode transaction) {
        logPayload(transaction);
        final String topic;
        try {
            topic = topicRouter.topicForType(type);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
        String notificationType = topicRouter.route(text(transaction, "entityName"), text(transaction, "actionName")).notificationType();
        if ("transaction".equalsIgnoreCase(type)) {
            notificationType = "TRANSACTION";
        } else if ("payment-confirmation".equalsIgnoreCase(type) || "payment_confirmation".equalsIgnoreCase(type)) {
            notificationType = "PAYMENT_CONFIRMATION";
        } else if ("account".equalsIgnoreCase(type)) {
            notificationType = "ACCOUNT";
        } else if ("card".equalsIgnoreCase(type)) {
            notificationType = "CARD";
        }
        TransactionEvent event = HookPayloads.toEvent(transaction, tenantId, notificationType, clientLookup);
        kafkaProducerService.send(topic, event);
        return ResponseEntity.ok("Hook received and sent to Kafka topic " + topic);
    }

    private void logPayload(JsonNode transaction) {
        String entity = text(transaction, "entityName");
        String action = text(transaction, "actionName");
        log.info("Fineract hook {} {} {}", entity, action, transaction);
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }
}
