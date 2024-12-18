package org.fivy.authservice.service;

import org.fivy.authservice.dto.LoginRequest;
import org.fivy.authservice.dto.TokenResponse;
import org.fivy.authservice.dto.UserRegistrationRequest;

public interface KeycloakAdminClientService {
    void registerUser(UserRegistrationRequest registerRequest);

    void resendVerificationEmail(String userId);

    TokenResponse login(LoginRequest request);

    boolean isEmailVerified(String username);

    void logout(String userId, String refreshToken);

    void resetPassword(String userId, String newPassword);

    void forgotPassword(String userEmail);
}
