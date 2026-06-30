package com.aidflow.campaign.infrastructure.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.aidflow.campaign.support.PostgresTestContainer;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "campaign.outbox.publisher.enabled=false"
})
@EmbeddedKafka(partitions = 1, topics = "campaign-created")
class OutboxEventPublisherIntegrationTest {
    @Autowired
    private SpringDataOutboxEventRepository repository;

    @Autowired
    private OutboxEventPublisher publisher;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    void publishesPendingEventToKafkaAndMarksItPublished() {
        repository.deleteAll();

        UUID eventId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        String payload = """
                {
                  "eventId": "%s",
                  "eventType": "CampaignCreated",
                  "campaignId": "%s"
                }
                """.formatted(eventId, campaignId);

        repository.save(OutboxEventJpaEntity.pending(
                eventId,
                "Campaign",
                campaignId,
                "CampaignCreated",
                "campaign-created",
                payload,
                OffsetDateTime.now(ZoneOffset.UTC)
        ));

        Consumer<String, String> consumer = createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, "campaign-created");

        publisher.publishPendingEvents();

        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(
                consumer,
                "campaign-created",
                Duration.ofSeconds(10)
        );

        assertThat(record.key()).isEqualTo(campaignId.toString());
        assertThat(record.value()).contains("\"eventType\": \"CampaignCreated\"");
        assertThat(record.value()).contains(campaignId.toString());

        OutboxEventJpaEntity publishedEvent = repository.findById(eventId).orElseThrow();
        assertThat(publishedEvent.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(publishedEvent.getPublishedAt()).isNotNull();
        assertThat(publishedEvent.getFailureReason()).isNull();

        consumer.close();
    }

    private Consumer<String, String> createConsumer() {
        Map<String, Object> props = KafkaTestUtils.consumerProps(
                "campaign-service-outbox-test",
                "false",
                embeddedKafkaBroker
        );
        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new StringDeserializer()
        ).createConsumer();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestContainer.registerProperties(registry);
    }
}
