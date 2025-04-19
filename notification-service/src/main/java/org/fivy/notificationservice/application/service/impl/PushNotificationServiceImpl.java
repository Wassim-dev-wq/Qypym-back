package org.fivy.notificationservice.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.notificationservice.application.service.PushNotificationService;
import org.fivy.notificationservice.application.service.UserNotificationPreferencesService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationServiceImpl implements PushNotificationService {

    private final RestTemplate restTemplate;
    private final UserNotificationPreferencesService preferencesService;
    private static final String EXPO_PUSH_API = "https://exp.host/--/api/v2/push/send";

    @Override
    public void sendNotification(String expoToken, String title, String body, Map<String, String> data) {
        sendNotification(null, expoToken, title, body, data);
    }

    @Override
    public void sendNotification(UUID userId, String expoToken, String title, String body, Map<String, String> data) {
        if (userId != null) {
            String notificationType = data.getOrDefault("type", "UNKNOWN");
            if (!shouldSendPushNotification(userId, notificationType)) {
                log.debug("Push notification type {} disabled for user {}", notificationType, userId);
                return;
            }
        }

        try {
            Map<String, Object> message = new HashMap<>();
            message.put("to", expoToken);
            message.put("title", title);
            message.put("body", body);
            message.put("data", data);
            message.put("sound", "default");
            message.put("priority", "high");
            String chatRoomId = data.getOrDefault("chatRoomId", null);
            String notificationId = data.getOrDefault("notificationId", null);
            if (notificationId != null) {
                log.debug("Configuring notification grouping with ID: {}", notificationId);
                message.put("identifier", notificationId);
                message.put("threadId", "chat_" + chatRoomId);
                message.put("channelId", "chat_messages");
                message.put("androidCollapsedTitle", title);
                message.put("tag", notificationId);
                message.put("collapseId", notificationId);
                Map<String, Object> iosCategory = new HashMap<>();
                iosCategory.put("body", true);
                iosCategory.put("badge", true);
                message.put("_category", iosCategory);
                try {
                    message.put("badge", Integer.parseInt(data.getOrDefault("messageCount", "1")));
                } catch (NumberFormatException e) {
                    message.put("badge", 1);
                }
            } else {
                message.put("badge", 1);
                message.put("channelId", "default_channel");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(message, headers);
            log.debug("Sending notification with parameters: {}", message);
            restTemplate.postForEntity(EXPO_PUSH_API, request, String.class);
            log.info("Notification sent successfully: token={}, title='{}'",
                    expoToken.substring(0, Math.min(10, expoToken.length())) + "...", title);
        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage(), e);
        }
    }

    private boolean shouldSendPushNotification(UUID userId, String notificationType) {
        return switch (notificationType) {
            case "CHAT_MESSAGE" -> preferencesService.shouldSendPushChatMessage(userId);
            case "MATCH_UPDATE" -> preferencesService.shouldSendPushMatchUpdate(userId);
            case "MATCH_INVITATION" -> preferencesService.shouldSendPushMatchInvitation(userId);
            case "MATCH_JOIN_REQUEST" -> preferencesService.shouldSendPushMatchJoinRequest(userId);
            case "MATCH_REMINDER" -> preferencesService.shouldSendPushMatchReminder(userId);
            case "TEAM_UPDATE" -> preferencesService.shouldSendPushTeamUpdate(userId);
            default -> true;
        };
    }
}