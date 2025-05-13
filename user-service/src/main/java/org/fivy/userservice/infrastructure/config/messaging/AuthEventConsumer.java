package org.fivy.userservice.infrastructure.config.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.fivy.userservice.api.dto.UserResponseDTO;
import org.fivy.userservice.application.service.UserService;
import org.fivy.userservice.domain.entity.User;
import org.fivy.userservice.domain.event.authEvent.AuthEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthEventConsumer {
    private final UserService userService;

    @KafkaListener(
            topics = "auth-events",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAuthEvent(ConsumerRecord<String, AuthEvent> record, Acknowledgment ack) {
        try {
            AuthEvent event = record.value();
            log.info("Received auth event: {}", event);
            switch (event.getType()) {
                case USER_REGISTERED -> handleUserRegistration(event);
                case USER_UPDATED -> handleUserUpdate(event);
                case USER_DELETED -> handleUserDeletion(event);
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing auth event", e);
            ack.acknowledge();
        }
    }

    private void handleUserRegistration(AuthEvent event) {
        try {
            Map<String, Object> userData = event.getData();
            User user = User.builder()
                    .keycloakUserId(event.getUserId())
                    .email((String) userData.get("email"))
                    .firstName((String) userData.get("firstName"))
                    .lastName((String) userData.get("lastName"))
                    .build();
            UserResponseDTO response = userService.createUserFromAuthEvent(user);
            log.info("Successfully created user profile from auth event. UserId: {}", response.getId());
        } catch (Exception e) {
            log.error("Failed to process user registration event for keycloakUserId: {}", event.getUserId(), e);
        }
    }

    private void handleUserUpdate(AuthEvent event) {
        log.info("Handling user update event");
    }

    private void handleUserDeletion(AuthEvent event) {
        log.info("Handling user deletion event");
    }
}