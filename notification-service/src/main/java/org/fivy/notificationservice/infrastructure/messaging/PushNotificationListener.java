package org.fivy.notificationservice.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.fivy.notificationservice.api.dto.response.UserPushTokenResponse;
import org.fivy.notificationservice.application.service.NotificationService;
import org.fivy.notificationservice.application.service.UserPushTokenService;
import org.fivy.notificationservice.domain.event.PushNotification;
import org.fivy.notificationservice.infrastructure.config.KafkaConsumerConfig;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PushNotificationListener {

    private final NotificationService notificationService;
    private final UserPushTokenService userPushTokenService;

    @KafkaListener(
            topics = KafkaConsumerConfig.TOPIC_PUSH_NOTIFICATIONS,
            containerFactory = "pushNotificationListenerContainerFactory"
    )
    public void listen(ConsumerRecord<String, PushNotification> record, Acknowledgment ack) {
        try {
            PushNotification notification = record.value();
            log.info("Reception of notification for user: {} - Type: {}",
                    notification.getUserId(), notification.getType());
            UUID userId = UUID.fromString(notification.getUserId());

            notificationService.sendNotification(
                    userId,
                    UUID.fromString(notification.getMatchId()),
                    notification.getMatchCreatorId() != null ? UUID.fromString(notification.getMatchCreatorId()) : null,
                    notification.getType(),
                    notification.getTitle(),
                    notification.getBody()
            );
            List<UserPushTokenResponse> userTokens = userPushTokenService.getTokensByUserId(userId);

            if (userTokens.isEmpty()) {
                log.warn("No token was found for user: {}", userId);
            } else {
                for (UserPushTokenResponse token : userTokens) {
                    try {
                        notificationService.sendPushNotification(
                                token.getExpoToken(),
                                notification.getTitle(),
                                notification.getBody()
                        );
                        log.debug("Notification sent to token: {}", token.getExpoToken());
                    } catch (Exception e) {
                        log.error("ERROR while sending notification to token: {}", token.getExpoToken(), e);
                    }
                }
            }

            ack.acknowledge();
            log.debug("Notification treated and confirmed");

        } catch (Exception e) {
            log.error("Erreur lors du traitement de la notification push", e);
            ack.acknowledge();
        }
    }
}