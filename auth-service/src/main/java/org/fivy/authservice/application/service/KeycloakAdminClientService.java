package org.fivy.authservice.application.service;

import org.fivy.authservice.api.dto.request.LoginRequest;
import org.fivy.authservice.api.dto.request.UserRegistrationRequest;
import org.fivy.authservice.api.dto.response.RefreshTokenResponse;
import org.fivy.authservice.api.dto.response.RegistrationResponse;
import org.fivy.authservice.api.dto.response.TokenResponse;

public interface KeycloakAdminClientService {
    RegistrationResponse registerUser(UserRegistrationRequest registerRequest);

    boolean verifyResetCode(String code, String userId);

    void resetPassword(String userId, String code, String newPassword);

    TokenResponse login(LoginRequest request);
    void logout(String userId, String refreshToken);

    boolean verifyToken(String token);
    boolean isEmailVerified(String email);
    RefreshTokenResponse refreshToken(String refreshToken);
    void verifyEmail(String code, String userId);
    String initiatePasswordReset(String email);
    void deleteAccount(String userId);
    boolean validateTokenBelongsToUser(String token, String userId);
}
