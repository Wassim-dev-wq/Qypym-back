package org.fivy.matchservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.api.dto.response.NotificationResponse;
import org.fivy.matchservice.application.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    @Operation(summary = "Send a notification", description = "Sends a notification to a specified user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public org.fivy.matchservice.api.dto.ApiResponse<NotificationResponse> sendNotification(
            @RequestParam UUID userId,
            @RequestParam String title,
            @RequestParam String message
    ) {
        log.info("Sending notification to user: {}", userId);
        NotificationResponse response = notificationService.sendNotification(userId, title, message);
        return org.fivy.matchservice.api.dto.ApiResponse.success(response);
    }

    @GetMapping("")
    @Operation(summary = "Get notifications", description = "Retrieves all notifications for the current user")
    public org.fivy.matchservice.api.dto.ApiResponse<Page<NotificationResponse>> getNotifications(
            @RequestHeader("X-User-ID") String userId,
            Pageable pageable
    ) {
        UUID currentUserId = UUID.fromString(userId);
        Page<NotificationResponse> notifications = notificationService.getNotifications(currentUserId, pageable);
        return org.fivy.matchservice.api.dto.ApiResponse.success(notifications);
    }

    @GetMapping("/unreadCount")
    @Operation(summary = "Get unread notifications count", description = "Retrieves the count of unread notifications for the current user")
    public org.fivy.matchservice.api.dto.ApiResponse<Long> getUnreadCount(
            @RequestHeader("X-User-ID") String userId
    ) {
        UUID currentUserId = UUID.fromString(userId);
        long count = notificationService.getUnreadCount(currentUserId);
        return org.fivy.matchservice.api.dto.ApiResponse.success(count);
    }

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark notification as read", description = "Marks a specific notification as read")
    public org.fivy.matchservice.api.dto.ApiResponse<NotificationResponse> markNotificationAsRead(
            @PathVariable UUID notificationId,
            @RequestHeader("X-User-ID") String userId
    ) {
        UUID currentUserId = UUID.fromString(userId);
        NotificationResponse response = notificationService.markAsRead(notificationId, currentUserId);
        return org.fivy.matchservice.api.dto.ApiResponse.success(response);
    }

    @PutMapping("/readAll")
    @Operation(summary = "Mark all notifications as read", description = "Marks all notifications as read for the current user")
    public org.fivy.matchservice.api.dto.ApiResponse<Void> markAllNotificationsAsRead(
            @RequestHeader("X-User-ID") String userId
    ) {
        UUID currentUserId = UUID.fromString(userId);
        notificationService.markAllAsRead(currentUserId);
        return org.fivy.matchservice.api.dto.ApiResponse.success(null);
    }
}
