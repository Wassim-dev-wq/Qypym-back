package org.fivy.authservice.application.service;

import org.fivy.authservice.api.dto.LoginRequest;
import org.fivy.authservice.api.dto.TokenResponse;
import org.fivy.authservice.api.dto.UserRegistrationRequest;

public interface KeycloakAdminClientService {
    String registerUser(UserRegistrationRequest registerRequest);
    void resendVerificationEmail(String userId);
    TokenResponse login(LoginRequest request);
    void logout(String userId, String refreshToken);
    void resetPassword(String userId, String newPassword);
    void forgotPassword(String userEmail);
    void verifyEmail(String token);
    void initiatePasswordReset(String email);
    boolean isEmailVerified(String email);
}
