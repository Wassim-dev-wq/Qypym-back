package org.fivy.notificationservice.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.notificationservice.application.service.PushNotificationService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationServiceImpl implements PushNotificationService {

    private final RestTemplate restTemplate;
    private static final String EXPO_PUSH_API = "https://exp.host/--/api/v2/push/send";

    @Override
    public void sendNotification(String expoToken, String title, String body, Map<String, String> data) {
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
                message.put("channelId", "chat_messages");
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
}