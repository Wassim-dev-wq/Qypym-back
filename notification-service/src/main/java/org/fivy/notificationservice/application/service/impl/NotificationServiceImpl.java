package org.fivy.notificationservice.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.notificationservice.api.dto.request.SupportRequestDto;
import org.fivy.notificationservice.api.dto.response.NotificationResponse;
import org.fivy.notificationservice.application.service.NotificationService;
import org.fivy.notificationservice.application.service.UserNotificationPreferencesService;
import org.fivy.notificationservice.domain.entity.Notification;
import org.fivy.notificationservice.domain.enums.NotificationType;
import org.fivy.notificationservice.domain.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final RestTemplate restTemplate;
    private final UserNotificationPreferencesService preferencesService;

    @Value("${expo.push.url:https://exp.host/--/api/v2/push/send}")
    private String expoPushUrl;

    @Value("${expo.push.timeout:5000}")
    private int expoPushTimeout;
    private final EmailService emailService;

    @Override
    @Transactional
    public NotificationResponse sendNotification(UUID userId, UUID matchId, UUID matchCreatorId, NotificationType type, String title, String message) {
        log.debug("Saving DB notification for user {}: {} - {}", userId, title, message);
        Notification notification = Notification.builder()
                .userId(userId)
                .matchId(matchId)
                .matchCreatorId(matchCreatorId)
                .type(type)
                .title(title)
                .message(message)
                .read(false)
                .build();
        Notification saved = notificationRepository.save(notification);
        log.info("Notification saved in DB for user {}: {} - {}", userId, title, message);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void sendPushNotification(String expoPushToken, String title, String message) {
        sendPushNotification(null, expoPushToken, title, message);
    }

    @Override
    @Transactional
    public void sendPushNotification(UUID userId, String expoPushToken, String title, String message) {
        if (userId != null && !preferencesService.shouldSendPushMatchUpdate(userId)) {
            log.debug("Push notifications disabled for user {}", userId);
            return;
        }
        log.debug("Sending push notification via Expo: {} - {}", title, message);
        Map<String, Object> payload = new HashMap<>();
        payload.put("to", expoPushToken);
        payload.put("title", title);
        payload.put("body", message);
        payload.put("sound", "default");
        payload.put("priority", "high");
        Map<String, Object> data = new HashMap<>();
        data.put("clickAction", "FLUTTER_NOTIFICATION_CLICK");
        data.put("title", title);
        data.put("body", message);
        payload.put("data", data);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(expoPushUrl, request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Expo push notification sent successfully, response: {}", response.getBody());
            } else {
                log.warn("Failed to send Expo push notification, status: {}, response: {}",
                        response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Error sending push notification via Expo", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(UUID userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return new PageImpl<>(
                notifications.getContent().stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList()),
                pageable,
                notifications.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to modify this notification");
        }
        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        log.info("Marking all notifications as read for user: {}", userId);
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Override
    public void sendSupportEmail(SupportRequestDto request) {
        emailService.sendSupportEmail(request);
    }
    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .matchId(notification.getMatchId())
                .matchCreatorId(notification.getMatchCreatorId())
                .type(notification.getType())
                .userId(notification.getUserId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}