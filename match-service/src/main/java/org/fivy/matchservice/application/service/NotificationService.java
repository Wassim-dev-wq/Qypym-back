package org.fivy.matchservice.application.service;

import org.fivy.matchservice.api.dto.response.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface NotificationService {
    NotificationResponse sendNotification(UUID userId, String title, String message);

    @Transactional
    void sendPushNotification(String expoPushToken, String title, String message);

    Page<NotificationResponse> getNotifications(UUID userId, Pageable pageable);
    long getUnreadCount(UUID userId);
    NotificationResponse markAsRead(UUID notificationId, UUID userId);
    void markAllAsRead(UUID userId);
}
