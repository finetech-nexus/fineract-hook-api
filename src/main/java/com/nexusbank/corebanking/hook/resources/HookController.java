package com.nexusbank.corebanking.hook.resources;


import com.nexusbank.corebanking.hook.model.TransactionPayload;
import com.nexusbank.corebanking.hook.services.KafkaProducerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hooks")
public class HookController {

    private final KafkaProducerService kafkaProducerService;

    public HookController(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    @PostMapping("/transactions")
    public ResponseEntity<String> receiveHook(@RequestBody TransactionPayload payload) {
        kafkaProducerService.sendEvent(kafkaProducerService.buildEvent(payload));
        return ResponseEntity.ok("Hook received and sent to Kafka");
    }
}
