package org.fivy.notificationservice.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.fivy.notificationservice.application.service.MatchEmailService;
import org.fivy.notificationservice.domain.event.email.MatchEmailEvent;
import org.fivy.notificationservice.infrastructure.config.KafkaConsumerConfig;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MatchEmailListener {
    private final MatchEmailService matchEmailService;
    @KafkaListener(
            topics = KafkaConsumerConfig.TOPIC_MATCH_EMAILS,
            containerFactory = "matchEmailListenerContainerFactory"
    )
    public void listen(ConsumerRecord<String, MatchEmailEvent> record, Acknowledgment ack) {
        try {
            MatchEmailEvent event = record.value();
            log.info("Received match email event of type '{}' for: {}",
                    event.getEventType(), event.getEmail());

            boolean sent = false;
            if ("MATCH_VERIFICATION".equals(event.getEventType())) {
                sent = matchEmailService.sendVerificationCodeEmail(event);
            } else if ("MATCH_REMINDER".equals(event.getEventType())) {
                sent = matchEmailService.sendMatchReminderEmail(event);
            } else {
                log.warn("Unknown match email event type: {}", event.getEventType());
            }

            if (sent) {
                log.info("Match email of type '{}' sent successfully to: {}",
                        event.getEventType(), event.getEmail());
            } else {
                log.error("Failed to send match email of type '{}' to: {}",
                        event.getEventType(), event.getEmail());
            }
            ack.acknowledge();
            log.debug("Match email event processed and acknowledged");
        } catch (Exception e) {
            log.error("Error processing match email event", e);
            ack.acknowledge();
        }
    }
}