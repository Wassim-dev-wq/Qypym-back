package org.fivy.userservice.infrastructure.config.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.fivy.userservice.domain.entity.User;
import org.fivy.userservice.domain.event.email.MatchEmailEvent;
import org.fivy.userservice.domain.event.email.MatchReminderEvent;
import org.fivy.userservice.domain.event.email.MatchVerificationCodeEvent;
import org.fivy.userservice.domain.repository.UserRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchEmailProcessor {

    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_EMAIL_VERIFICATION = "email-verification-events";
    private static final String TOPIC_MATCH_EMAILS = "match-email-events";


    @KafkaListener(
            topics = "match-verification-events",
            containerFactory = "matchVerificationListenerContainerFactory"
    )
    public void handleMatchVerificationEvent(ConsumerRecord<String, MatchVerificationCodeEvent> record, Acknowledgment ack) {
        try {
            MatchVerificationCodeEvent event = record.value();
            log.debug("Received match verification code event for match: {}, creator: {}",
                    event.getMatchId(), event.getCreatorId());
            Optional<User> userOpt = userRepository.findByKeycloakUserId(String.valueOf(event.getCreatorId()));
            if (userOpt.isEmpty()) {
                log.error("Could not find user for creator ID: {}", event.getCreatorId());
                ack.acknowledge();
                return;
            }
            User user = userOpt.get();
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                log.error("User has no email address: {}", event.getCreatorId());
                ack.acknowledge();
                return;
            }

            MatchEmailEvent matchEmailEvent = MatchEmailEvent.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .eventType("MATCH_VERIFICATION")
                    .matchId(event.getMatchId())
                    .matchTitle(event.getMatchTitle())
                    .matchStartDate(event.getMatchStartDate())
                    .matchLocation(event.getMatchLocation())
                    .matchFormat(event.getMatchFormat())
                    .verificationCode(event.getVerificationCode())
                    .verificationCodeTtl(event.getVerificationCodeTtl())
                    .build();

            kafkaTemplate.send(TOPIC_MATCH_EMAILS, user.getId().toString(), matchEmailEvent)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish match email event for user: {}", user.getId(), ex);
                        } else {
                            log.debug("Published match email event for user: {}", user.getId());
                        }
                    });

            ack.acknowledge();
            log.info("Processed verification code event for match: {} and user: {}",
                    event.getMatchId(), user.getId());
        } catch (Exception e) {
            log.error("Error processing match verification event", e);
            ack.acknowledge();
        }
    }

    @KafkaListener(
            topics = "match-reminder-events",
            containerFactory = "matchReminderListenerContainerFactory"
    )
    public void handleMatchReminderEvent(ConsumerRecord<String, MatchReminderEvent> record, Acknowledgment ack) {
        try {
            MatchReminderEvent event = record.value();
            log.debug("Received match reminder event for match: {}, player: {}",
                    event.getMatchId(), event.getPlayerId());
            Optional<User> userOpt = userRepository.findById(event.getPlayerId());
            if (userOpt.isEmpty()) {
                log.error("Could not find user for player ID: {}", event.getPlayerId());
                ack.acknowledge();
                return;
            }

            User user = userOpt.get();
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                log.error("User has no email address: {}", event.getPlayerId());
                ack.acknowledge();
                return;
            }

            MatchEmailEvent matchEmailEvent = MatchEmailEvent.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .eventType("MATCH_REMINDER")
                    .matchId(event.getMatchId())
                    .matchTitle(event.getMatchTitle())
                    .matchStartDate(event.getMatchStartDate())
                    .matchLocation(event.getMatchLocation())
                    .matchFormat(event.getMatchFormat())
                    .teamName(event.getTeamName())
                    .playerRole(event.getPlayerRole())
                    .build();
            kafkaTemplate.send(TOPIC_MATCH_EMAILS, user.getId().toString(), matchEmailEvent)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish match reminder email event for user: {}", user.getId(), ex);
                        } else {
                            log.debug("Published match reminder email event for user: {}", user.getId());
                        }
                    });
            ack.acknowledge();
            log.info("Processed reminder event for match: {} and user: {}",
                    event.getMatchId(), user.getId());
        } catch (Exception e) {
            log.error("Error processing match reminder event", e);
            ack.acknowledge();
        }
    }
}