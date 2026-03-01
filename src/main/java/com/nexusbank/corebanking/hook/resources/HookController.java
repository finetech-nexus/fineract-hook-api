package com.nexusbank.corebanking.hook.resources;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusbank.corebanking.hook.model.Transaction;
import com.nexusbank.corebanking.hook.model.TransactionEvent;
import com.nexusbank.corebanking.hook.model.TransactionPayload;
import com.nexusbank.corebanking.hook.model.TransactionRequest;
import com.nexusbank.corebanking.hook.services.KafkaProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/hooks")
public class HookController {

    private static final Logger log = LoggerFactory.getLogger(HookController.class);
    private final KafkaProducerService kafkaProducerService;
    @Value("${fineract.tenantId}")
    private String tenantId;

    public HookController(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    @PostMapping("/transactions/")
    public ResponseEntity<String> receiveHook(@RequestBody Transaction transaction) {
        TransactionPayload transactionPayload = new TransactionPayload();
        try {
            log.info(new ObjectMapper().writeValueAsString(transaction));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        transactionPayload.setDate(transaction.getRequest().getTransactionDate());
        transactionPayload.setTransactionId(String.valueOf(transaction.getResponse().getResourceId()));
        transactionPayload.setNote(transaction.getRequest().getNote());
        transactionPayload.setAmount(String.valueOf(transaction.getRequest().getTransactionAmount()));
        transactionPayload.setTransactionType(transaction.getActionName());
        transactionPayload.setDate(transaction.getRequest().getTransactionDate());
        transactionPayload.setClientId(String.valueOf(transaction.getResponse().getClientId()));
        transactionPayload.setAccountId(String.valueOf(transaction.getResponse().getSavingsId()));
        //transactionPayload.setCurrency(transaction.getRequest().);
        TransactionEvent event = new TransactionEvent();
        event.setPayload(transactionPayload);
        event.setAction("CREATION");
        event.setEntity(transaction.getEntityName());
        event.setResourceId(String.valueOf(transaction.getResponse().getResourceId()));
        event.setTenantId(tenantId);
        kafkaProducerService.sendEvent(event);
        return ResponseEntity.ok("Hook received and sent to Kafka");
    }
}
