package org.fivy.authservice.application.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.authservice.api.dto.request.LoginRequest;
import org.fivy.authservice.api.dto.request.UserRegistrationRequest;
import org.fivy.authservice.api.dto.response.RefreshTokenResponse;
import org.fivy.authservice.api.dto.response.TokenResponse;
import org.fivy.authservice.application.service.KeycloakAdminClientService;
import org.fivy.authservice.domain.enums.AuthEventType;
import org.fivy.authservice.domain.event.AuthEvent;
import org.fivy.authservice.infrastructure.client.KeycloakClient;
import org.fivy.authservice.shared.exception.AuthException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class KeycloakAdminClientServiceImpl implements KeycloakAdminClientService {
    private static final String EMAIL_VERIFICATION_CACHE_PREFIX = "email_verification:";
    private static final long EMAIL_VERIFICATION_TTL = 24;
    private final KeycloakClient keycloakClient;
    private final KafkaTemplate<String, AuthEvent> kafkaTemplate;
    private final StringRedisTemplate redisTemplate;

    @Override
    public String registerUser(UserRegistrationRequest request) {
        log.info("Starting user registration process for email: {}", request.getEmail());

        validateUniqueEmail(request.getEmail());

        String userId = keycloakClient.createUser(request);

        try {
            AuthEvent event = AuthEvent.builder()
                    .userId(userId)
                    .type(AuthEventType.USER_REGISTERED)
                    .data(Map.of(
                            "email", request.getEmail(),
                            "username", request.getUsername(),
                            "firstName", request.getFirstName(),
                            "lastName", request.getLastName()
                    ))
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaTemplate.send("auth-events", userId, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("Published user registered event for userId: {}", userId);
                        } else {
                            log.error("Failed to publish user registered event for userId: {}", userId, ex);
                        }
                    });
            keycloakClient.sendVerificationEmail(userId);
            cacheEmailVerificationStatus(userId, false);
            publishUserRegisteredEvent(userId, request);
            log.info("User registration completed successfully for userId: {}", userId);
            return userId;
        } catch (Exception e) {
            log.error("Error during post-registration process for userId: {}", userId, e);
            attemptUserDeletion(userId);
            throw new AuthException(
                    "Registration process failed",
                    "REG_FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e
            );
        }
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        log.info("Processing login request for email: {}", request.getEmail());

        if (!isEmailVerified(request.getEmail())) {
            throw new AuthException(
                    "Email not verified",
                    "EMAIL_NOT_VERIFIED",
                    HttpStatus.FORBIDDEN
            );
        }

        TokenResponse tokenResponse = keycloakClient.getToken(request);
        publishUserLoggedInEvent(request.getEmail());

        return tokenResponse;
    }

    @Override
    public void logout(String userId, String refreshToken) {
        log.info("Processing logout request for userId: {}", userId);

        try {
            keycloakClient.logout(userId, refreshToken);
            publishUserLoggedOutEvent(userId);
        } catch (Exception e) {
            log.error("Error during logout for userId: {}", userId, e);
            throw new AuthException(
                    "Logout failed",
                    "LOGOUT_FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e
            );
        }
    }

    @Override
    public void resetPassword(String userId, String newPassword) {

    }

    @Override
    public void forgotPassword(String userEmail) {

    }

    @Override
    public void resendVerificationEmail(String userId) {
        log.info("Resending verification email for userId: {}", userId);

        try {
            keycloakClient.sendVerificationEmail(userId);
        } catch (Exception e) {
            log.error("Failed to resend verification email for userId: {}", userId, e);
            throw new AuthException(
                    "Failed to resend verification email",
                    "EMAIL_RESEND_FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e
            );
        }
    }

    private void validateUniqueEmail(String email) {
        if (keycloakClient.emailExists(email)) {
            throw new AuthException(
                    "Email already registered",
                    "EMAIL_EXISTS",
                    HttpStatus.CONFLICT
            );
        }
    }

    private void cacheEmailVerificationStatus(String userId, boolean verified) {
        String cacheKey = EMAIL_VERIFICATION_CACHE_PREFIX + userId;
        redisTemplate.opsForValue().set(
                cacheKey,
                String.valueOf(verified),
                EMAIL_VERIFICATION_TTL,
                TimeUnit.HOURS
        );
    }

    public boolean isEmailVerified(String email) {
        String userId = keycloakClient.getUserIdByEmail(email);
        String cacheKey = EMAIL_VERIFICATION_CACHE_PREFIX + userId;

        String cachedStatus = redisTemplate.opsForValue().get(cacheKey);
        if (cachedStatus != null) {
            return Boolean.parseBoolean(cachedStatus);
        }

        boolean verified = keycloakClient.isEmailVerified(userId);
        cacheEmailVerificationStatus(userId, verified);
        return verified;
    }

    @Override
    public RefreshTokenResponse refreshToken(String refreshToken) {
        log.info("Processing refresh token request");

        try {
            RefreshTokenResponse tokenResponse = keycloakClient.refreshToken(refreshToken);
            return tokenResponse;

        } catch (Exception e) {
            log.error("Failed to refresh token", e);
            throw new AuthException(
                    "Token refresh failed",
                    "REFRESH_FAILED",
                    HttpStatus.UNAUTHORIZED,
                    e
            );
        }
    }

    private void publishUserRegisteredEvent(String userId, UserRegistrationRequest request) {
        AuthEvent event = AuthEvent.builder()
                .type(AuthEventType.USER_REGISTERED)
                .userId(userId)
                .data(Map.of(
                        "email", request.getEmail(),
                        "username", request.getUsername(),
                        "firstName", request.getFirstName(),
                        "lastName", request.getLastName()
                ))
                .build();

        kafkaTemplate.send("auth-events", userId, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("Published user registered event for userId: {}", userId);
                    } else {
                        log.error("Failed to publish user registered event for userId: {}", userId, ex);
                    }
                });
    }

    @Override
    public boolean verifyToken(String token) {
        log.info("Verifying access token");
        return keycloakClient.verifyToken(token);
    }

    private void publishUserLoggedInEvent(String email) {
        String userId = keycloakClient.getUserIdByEmail(email);
        AuthEvent event = AuthEvent.builder()
                .type(AuthEventType.USER_LOGGED_IN)
                .userId(userId)
                .data(Map.of("email", email))
                .build();

        kafkaTemplate.send("auth-events", userId, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish login event for user: {}", userId, ex);
                    }
                });
    }

    private void publishUserLoggedOutEvent(String userId) {
        AuthEvent event = AuthEvent.builder()
                .type(AuthEventType.USER_LOGGED_OUT)
                .userId(userId)
                .build();

        kafkaTemplate.send("auth-events", userId, event);
    }

    private void attemptUserDeletion(String userId) {
        try {
            keycloakClient.deleteUser(userId);
        } catch (Exception e) {
            log.error("Failed to delete user during rollback for userId: {}", userId, e);
        }
    }

    @Override
    public void verifyEmail(String token) {
        log.info("Processing email verification with token");
        try {
            String userId = keycloakClient.validateEmailToken(token);
            keycloakClient.updateEmailVerificationStatus(userId, true);
            cacheEmailVerificationStatus(userId, true);
            publishEmailVerifiedEvent(userId);

        } catch (Exception e) {
            log.error("Email verification failed", e);
            throw new AuthException(
                    "Email verification failed",
                    "VERIFY_FAILED",
                    HttpStatus.BAD_REQUEST,
                    e
            );
        }
    }

    @Override
    public void initiatePasswordReset(String email) {
        log.info("Initiating password reset for email: {}", email);
        try {
            String userId = keycloakClient.getUserIdByEmail(email);
            if (userId == null) {
                throw new AuthException(
                        "User not found",
                        "USER_NOT_FOUND",
                        HttpStatus.NOT_FOUND
                );
            }
            keycloakClient.sendPasswordResetEmail(userId);
            publishPasswordResetInitiatedEvent(userId, email);

        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to initiate password reset", e);
            throw new AuthException(
                    "Failed to initiate password reset",
                    "RESET_FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e
            );
        }
    }

    private void publishEmailVerifiedEvent(String userId) {
        AuthEvent event = AuthEvent.builder()
                .type(AuthEventType.EMAIL_VERIFIED)
                .userId(userId)
                .build();

        kafkaTemplate.send("auth-events", userId, event);
    }

    private void publishPasswordResetInitiatedEvent(String userId, String email) {
        AuthEvent event = AuthEvent.builder()
                .type(AuthEventType.PASSWORD_RESET_REQUESTED)
                .userId(userId)
                .data(Map.of("email", email))
                .build();

        kafkaTemplate.send("auth-events", userId, event);
    }
}