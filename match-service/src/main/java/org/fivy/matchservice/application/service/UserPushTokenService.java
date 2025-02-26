package org.fivy.matchservice.application.service;

import org.fivy.matchservice.api.dto.response.UserPushTokenResponse;
import org.fivy.matchservice.api.dto.request.UserPushTokenRequest;
import java.util.List;
import java.util.UUID;

public interface UserPushTokenService {
    UserPushTokenResponse registerToken(UserPushTokenRequest request);
    void deleteToken(UUID userId, String expoToken);
    List<UserPushTokenResponse> getTokensByUserId(UUID userId);
}
