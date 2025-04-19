package org.fivy.notificationservice.application.service;

import java.util.Map;
import java.util.UUID;

public interface PushNotificationService {
    void sendNotification(String expoToken, String title, String body, Map<String, String> data);

    void sendNotification(UUID userId, String expoToken, String title, String body, Map<String, String> data);
}
