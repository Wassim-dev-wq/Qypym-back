package org.fivy.matchservice.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.api.dto.response.NotificationResponse;
import org.fivy.matchservice.application.service.NotificationService;
import org.fivy.matchservice.domain.entity.Notification;
import org.fivy.matchservice.domain.repository.NotificationRepository;
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
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    @Transactional
    public NotificationResponse sendNotification(UUID userId, String title, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .read(false)
                .build();
        Notification saved = notificationRepository.save(notification);
        log.info("Notification saved in DB for user {}: {} - {}", userId, title, message);
        return NotificationResponse.builder()
                .id(saved.getId())
                .userId(saved.getUserId())
                .title(saved.getTitle())
                .message(saved.getMessage())
                .read(saved.isRead())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void sendPushNotification(String expoPushToken, String title, String message) {
        final String expoPushUrl = "https://exp.host/--/api/v2/push/send";

        Map<String, Object> payload = new HashMap<>();
        payload.put("to", expoPushToken);
        payload.put("title", title);
        payload.put("body", message);
        payload.put("data", Map.of("clickAction", "FLUTTER_NOTIFICATION_CLICK"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(expoPushUrl, request, String.class);
            log.info("Expo push notification sent, response: {}", response.getBody());
        } catch (Exception e) {
            log.error("Error sending push notification via Expo", e);
        }
    }

    @Transactional
    public NotificationResponse sendAndPushNotification(UUID userId, String expoPushToken, String title, String message) {
        NotificationResponse notificationResponse = sendNotification(userId, title, message);
        sendPushNotification(expoPushToken, title, message);
        return notificationResponse;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(UUID userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return new PageImpl<>(notifications.getContent().stream()
                .map(n -> NotificationResponse.builder()
                        .id(n.getId())
                        .userId(n.getUserId())
                        .title(n.getTitle())
                        .message(n.getMessage())
                        .read(n.isRead())
                        .createdAt(n.getCreatedAt())
                        .build())
                .collect(Collectors.toList()),
                pageable, notifications.getTotalElements());
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
            throw new RuntimeException("Not authorized to update this notification");
        }
        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        return NotificationResponse.builder()
                .id(saved.getId())
                .userId(saved.getUserId())
                .title(saved.getTitle())
                .message(saved.getMessage())
                .read(saved.isRead())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }
}
