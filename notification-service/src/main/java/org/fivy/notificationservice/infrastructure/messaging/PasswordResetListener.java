package org.fivy.notificationservice.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.fivy.notificationservice.application.service.impl.EmailService;
import org.fivy.notificationservice.domain.event.PasswordResetEvent;
import org.fivy.notificationservice.infrastructure.config.KafkaConsumerConfig;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordResetListener {

    private final EmailService emailService;

    @KafkaListener(
            topics = KafkaConsumerConfig.TOPIC_PASSWORD_RESET,
            containerFactory = "passwordResetListenerContainerFactory"
    )
    public void listen(ConsumerRecord<String, PasswordResetEvent> record, Acknowledgment ack) {
        try {
            PasswordResetEvent event = record.value();
            log.info("Received password reset request for: {}", event.getEmail());
            boolean sent = emailService.sendPasswordResetEmail(
                    event.getEmail(),
                    event.getResetCode(),
                    event.getResetCodeTtl()
            );
            if (sent) {
                log.info("Password reset email sent successfully to: {}", event.getEmail());
            } else {
                log.error("Failed to send password reset email to: {}", event.getEmail());
            }
            ack.acknowledge();
            log.debug("Password reset event processed and acknowledged");
        } catch (Exception e) {
            log.error("Error processing password reset event", e);
            ack.acknowledge();
        }
    }
}