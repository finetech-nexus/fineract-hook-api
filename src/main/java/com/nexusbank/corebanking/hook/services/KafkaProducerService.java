package com.nexusbank.corebanking.hook.services;

import com.nexusbank.corebanking.hook.model.TransactionEvent;
import com.nexusbank.corebanking.hook.model.TransactionPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {


    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${fineract.kafka.topic.transaction}")
    private String topic;

    public void sendEvent(TransactionEvent event) {
        String key = event.getTenantIdentifier() + "-" + event.getResourceId();
        String value = event.toString();
        kafkaTemplate.send(topic, key, value);
    }

    public TransactionEvent buildEvent(TransactionPayload payload) {
        TransactionEvent e = new TransactionEvent();
        e.setPayload(payload);
        return e;
    }
}



