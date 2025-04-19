package org.fivy.notificationservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.notificationservice.api.dto.request.SupportRequestDto;
import org.fivy.notificationservice.api.dto.response.NotificationResponse;
import org.fivy.notificationservice.application.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static org.fivy.notificationservice.shared.JwtConverter.extractUserIdFromJwt;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Validated
@SecurityRequirement(name = "bearer-auth")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("")
    @Operation(summary = "Get notifications", description = "Retrieves all notifications for the current user")
    public org.fivy.notificationservice.api.dto.ApiResponse<Page<NotificationResponse>> getNotifications(
            Authentication authentication,
            Pageable pageable
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("Fetching notifications for user: {}", currentUserId);
        Page<NotificationResponse> notifications = notificationService.getNotifications(currentUserId, pageable);
        return org.fivy.notificationservice.api.dto.ApiResponse.success(notifications);
    }

    @GetMapping("/unreadCount")
    @Operation(summary = "Get unread notifications count", description = "Retrieves the count of unread notifications for the current user")
    public org.fivy.notificationservice.api.dto.ApiResponse<Long> getUnreadCount(
            Authentication authentication
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("Fetching unread notification count for user: {}", currentUserId);
        long count = notificationService.getUnreadCount(currentUserId);
        return org.fivy.notificationservice.api.dto.ApiResponse.success(count);
    }

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark notification as read", description = "Marks a specific notification as read")
    public org.fivy.notificationservice.api.dto.ApiResponse<NotificationResponse> markNotificationAsRead(
            @PathVariable UUID notificationId,
            Authentication authentication
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("Marking notification {} as read for user: {}", notificationId, currentUserId);
        NotificationResponse response = notificationService.markAsRead(notificationId, currentUserId);
        return org.fivy.notificationservice.api.dto.ApiResponse.success(response);
    }

    @PutMapping("/readAll")
    @Operation(summary = "Mark all notifications as read", description = "Marks all notifications as read for the current user")
    public org.fivy.notificationservice.api.dto.ApiResponse<Void> markAllNotificationsAsRead(
            Authentication authentication
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("Marking all notifications as read for user: {}", currentUserId);
        notificationService.markAllAsRead(currentUserId);
        return org.fivy.notificationservice.api.dto.ApiResponse.success(null);
    }

    @PostMapping("/support")
    @Operation(summary = "Submit a support request", description = "Submits a support request and sends an email to support team")
    public org.fivy.notificationservice.api.dto.ApiResponse<Void> submitSupportRequest(
            @RequestBody @Valid SupportRequestDto request,
            Authentication authentication
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("Submitting support request from user: {}", currentUserId);
        notificationService.sendSupportEmail(request);
        return org.fivy.notificationservice.api.dto.ApiResponse.success(null);
    }
}