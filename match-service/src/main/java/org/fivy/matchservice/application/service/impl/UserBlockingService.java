package org.fivy.matchservice.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.api.dto.ApiResponse;
import org.fivy.matchservice.infrastructure.client.UserServiceClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserBlockingService {
    private final UserServiceClient userServiceClient;

    public List<UUID> getBlockedUserIds(String authToken) {
        try {
            ApiResponse<List<UUID>> response = userServiceClient.getBlockedUserIdsWithWrapper(authToken);
            return response != null && response.getData() != null ? response.getData() : List.of();
        } catch (Exception e) {
            log.error("Error fetching blocked user IDs", e);
            return List.of();
        }
    }

    public boolean isUserBlocked(String authToken, UUID userId) {
        try {
            ApiResponse<Boolean> response = userServiceClient.isUserBlockedWithWrapper(authToken, userId);
            return response != null && Boolean.TRUE.equals(response.getData());
        } catch (Exception e) {
            log.error("Error checking if user is blocked", e);
            return false;
        }
    }
}