package org.fivy.notificationservice.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.notificationservice.api.dto.request.UserNotificationPreferencesRequest;
import org.fivy.notificationservice.api.dto.response.UserNotificationPreferencesResponse;
import org.fivy.notificationservice.application.service.UserNotificationPreferencesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static org.fivy.notificationservice.shared.JwtConverter.extractUserIdFromJwt;

@RestController
@RequestMapping("/api/v1/notification-preferences")
@RequiredArgsConstructor
@Slf4j
public class UserNotificationPreferencesController {

    private final UserNotificationPreferencesService preferencesService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserNotificationPreferencesResponse> getPreferences(@PathVariable UUID userId, Authentication authentication) {
        log.debug("Getting notification preferences for user: {}", userId);
        return ResponseEntity.ok(preferencesService.getPreferences(userId));
    }

    @PostMapping
    public ResponseEntity<UserNotificationPreferencesResponse> updatePreferences(
            @RequestBody UserNotificationPreferencesRequest request, Authentication authentication) {
        UUID userId = extractUserIdFromJwt(authentication);
        request.setUserId(userId);
        log.debug("Updating notification preferences for user: {}", request.getUserId());
        return ResponseEntity.ok(preferencesService.updatePreferences(request));
    }
}