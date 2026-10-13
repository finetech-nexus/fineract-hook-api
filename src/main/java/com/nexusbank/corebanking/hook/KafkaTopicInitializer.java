package com.nexusbank.corebanking.hook;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Creates the notification topics when they are missing so the {@code notification} consumer group can subscribe.
 */
@Component
public class KafkaTopicInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KafkaTopicInitializer.class);

    private final String bootstrapServers;
    private final List<String> topics;

    public KafkaTopicInitializer(
            @Value("${fineract.kafka.bootstrap-servers:}") String bootstrapServers,
            @Value("${fineract.kafka.topic.transaction:transactions}") String legacyTopic,
            @Value("${fineract.kafka.topic.notification-transaction:notification.transaction}") String transactionTopic,
            @Value("${fineract.kafka.topic.notification-payment-confirmation:notification.payment-confirmation}") String paymentTopic,
            @Value("${fineract.kafka.topic.notification-account:notification.account}") String accountTopic,
            @Value("${fineract.kafka.topic.notification-card:notification.card}") String cardTopic) {
        this.bootstrapServers = bootstrapServers == null ? "" : bootstrapServers.trim();
        LinkedHashSet<String> names = new LinkedHashSet<>();
        names.add(legacyTopic);
        names.add(transactionTopic);
        names.add(paymentTopic);
        names.add(accountTopic);
        names.add(cardTopic);
        names.add("notification.offer");
        names.add("notification.security");
        names.add("notification.reminder");
        names.remove("");
        this.topics = List.copyOf(names);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (bootstrapServers.isBlank()) {
            log.info("Kafka bootstrap servers are empty; notification topics were not created");
            return;
        }
        Exception last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                ensureTopics();
                return;
            } catch (Exception error) {
                last = error;
                log.warn("Could not ensure notification topics (attempt {}): {}", attempt, error.toString());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        log.warn("Notification topics were not created. The notification consumer group will not see them until Kafka is reachable. Last error: {}", last);
    }

    private void ensureTopics() throws Exception {
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "8000");
        properties.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "8000");
        try (AdminClient admin = AdminClient.create(properties)) {
            Set<String> existing = admin.listTopics().names().get(8, TimeUnit.SECONDS);
            int brokers = Math.max(1, admin.describeCluster().nodes().get(8, TimeUnit.SECONDS).size());
            short replicas = (short) Math.min(brokers, 3);
            List<NewTopic> missing = new ArrayList<>();
            for (String topic : topics) {
                if (!existing.contains(topic)) {
                    missing.add(new NewTopic(topic, 3, replicas));
                }
            }
            if (missing.isEmpty()) {
                log.info("Notification topics already exist for consumer group notification: {}", topics);
                return;
            }
            admin.createTopics(missing).all().get(15, TimeUnit.SECONDS);
            log.info("Created Kafka topics for consumer group notification: {}", missing.stream().map(NewTopic::name).toList());
        }
    }
}
