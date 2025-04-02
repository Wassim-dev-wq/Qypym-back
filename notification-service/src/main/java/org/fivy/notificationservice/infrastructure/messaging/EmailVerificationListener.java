package org.fivy.notificationservice.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.fivy.notificationservice.application.service.impl.EmailService;
import org.fivy.notificationservice.domain.event.EmailVerificationEvent;
import org.fivy.notificationservice.infrastructure.config.KafkaConsumerConfig;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationListener {

    private final EmailService emailService;

    @KafkaListener(
            topics = KafkaConsumerConfig.TOPIC_EMAIL_VERIFICATION,
            containerFactory = "emailVerificationListenerContainerFactory"
    )
    public void listen(ConsumerRecord<String, EmailVerificationEvent> record, Acknowledgment ack) {
        try {
            EmailVerificationEvent event = record.value();
            log.info("Received email verification request for: {}", event.getEmail());
            boolean sent = emailService.sendVerificationEmail(
                    event.getEmail(),
                    event.getVerificationCode(),
                    event.getVerificationCodeTtl()
            );
            if (sent) {
                log.info("Verification email sent successfully to: {}", event.getEmail());
            } else {
                log.error("Failed to send verification email to: {}", event.getEmail());
            }
            ack.acknowledge();
            log.debug("Email verification event processed and acknowledged");
        } catch (Exception e) {
            log.error("Error processing email verification event", e);
            ack.acknowledge();
        }
    }
}