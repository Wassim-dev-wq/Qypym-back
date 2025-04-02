package org.fivy.notificationservice.application.service;

import java.util.Map;

public interface PushNotificationService {
    void sendNotification(String expoToken, String title, String body, Map<String, String> data);
}
