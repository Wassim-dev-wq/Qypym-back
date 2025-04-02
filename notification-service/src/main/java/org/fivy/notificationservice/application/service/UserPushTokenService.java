package org.fivy.notificationservice.application.service;

import org.fivy.notificationservice.api.dto.request.UserPushTokenRequest;
import org.fivy.notificationservice.api.dto.response.UserPushTokenResponse;

import java.util.List;
import java.util.UUID;

public interface UserPushTokenService {
    UserPushTokenResponse registerToken(UserPushTokenRequest request);

    void deleteToken(UUID userId, String expoToken);

    List<UserPushTokenResponse> getTokensByUserId(UUID userId);
}
