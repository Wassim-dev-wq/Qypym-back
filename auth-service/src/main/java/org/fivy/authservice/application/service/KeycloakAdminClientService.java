package org.fivy.authservice.application.service;

import org.fivy.authservice.api.dto.request.LoginRequest;
import org.fivy.authservice.api.dto.request.UserRegistrationRequest;
import org.fivy.authservice.api.dto.response.RefreshTokenResponse;
import org.fivy.authservice.api.dto.response.TokenResponse;

public interface KeycloakAdminClientService {
    String registerUser(UserRegistrationRequest registerRequest);
    void resendVerificationEmail(String userId);
    TokenResponse login(LoginRequest request);
    void logout(String userId, String refreshToken);
    boolean verifyToken(String token);
    void resetPassword(String userId, String newPassword);
    void forgotPassword(String userEmail);
    void verifyEmail(String token);
    void initiatePasswordReset(String email);
    boolean isEmailVerified(String email);
    RefreshTokenResponse refreshToken(String refreshToken);

}
