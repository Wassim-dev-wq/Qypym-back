package org.fivy.userservice.infrastructure.config.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.userservice.domain.event.userEvent.UserEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventPublisher {
    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    public void publishUserEvent(UserEvent event) {
        String topic = "user-events";
        String key = event.getUserId().toString();

        log.info("Attempting to publish event to topic: {}, type: {}, userId: {}",
                topic, event.getType(), event.getUserId());

        try {
            kafkaTemplate.send(topic, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Successfully published event: topic={}, partition={}, offset={}",
                                    result.getRecordMetadata().topic(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to publish event: {}", event, ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Error publishing event: {}", event, e);
        }
    }
}