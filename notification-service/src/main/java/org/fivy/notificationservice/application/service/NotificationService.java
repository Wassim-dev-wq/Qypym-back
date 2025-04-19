package org.fivy.notificationservice.application.service;

import org.fivy.notificationservice.api.dto.request.SupportRequestDto;
import org.fivy.notificationservice.api.dto.response.NotificationResponse;
import org.fivy.notificationservice.domain.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface NotificationService {

    @Transactional
    NotificationResponse sendNotification(UUID userId, UUID matchId, UUID matchCreatorId, NotificationType type, String title, String message);

    @Transactional
    void sendPushNotification(String expoPushToken, String title, String message);

    @Transactional
    void sendPushNotification(UUID userId, String expoPushToken, String title, String message);

    Page<NotificationResponse> getNotifications(UUID userId, Pageable pageable);

    long getUnreadCount(UUID userId);

    NotificationResponse markAsRead(UUID notificationId, UUID userId);

    void markAllAsRead(UUID userId);

    void sendSupportEmail(SupportRequestDto request);
}
