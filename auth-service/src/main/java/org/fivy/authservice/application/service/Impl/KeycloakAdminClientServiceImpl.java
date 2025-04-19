package org.fivy.authservice.application.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.authservice.api.dto.request.LoginRequest;
import org.fivy.authservice.api.dto.request.UserRegistrationRequest;
import org.fivy.authservice.api.dto.response.RefreshTokenResponse;
import org.fivy.authservice.api.dto.response.RegistrationResponse;
import org.fivy.authservice.api.dto.response.TokenResponse;
import org.fivy.authservice.application.service.KeycloakAdminClientService;
import org.fivy.authservice.domain.enums.AuthEventType;
import org.fivy.authservice.domain.event.AuthEvent;
import org.fivy.authservice.domain.event.EmailVerificationEvent;
import org.fivy.authservice.domain.event.PasswordResetEvent;
import org.fivy.authservice.infrastructure.client.KeycloakClient;
import org.fivy.authservice.infrastructure.config.KafkaConfig;
import org.fivy.authservice.shared.exception.AuthException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class KeycloakAdminClientServiceImpl implements KeycloakAdminClientService {
    private static final String EMAIL_VERIFICATION_CODE_PREFIX = "email_verification_code:";
    private static final String PASSWORD_RESET_CODE_PREFIX = "password_reset_code:";
    private static final int CODE_EXPIRATION_MINUTES = 3;

    private final KeycloakClient keycloakClient;
    private final KafkaTemplate<String, AuthEvent> kafkaTemplate;
    private final KafkaTemplate<String, Object> genericKafkaTemplate;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, EmailVerificationEvent> kafkaTemplateEmail;
    private final KafkaTemplate<String, PasswordResetEvent> kafkaTemplatePasswordReset;
    private final SecureRandom random = new SecureRandom();

    @Override
    public RegistrationResponse registerUser(UserRegistrationRequest request) {
        log.info("Starting user registration process for email: {}", request.getEmail());
        validateUniqueEmail(request.getEmail());
        String userId = keycloakClient.createUser(request);
        try {
            String verificationCode = generateAndCacheVerificationCode(EMAIL_VERIFICATION_CODE_PREFIX, userId);
            publishUserRegisteredEvent(userId, request);
            publishVerificationCodeEvent(userId, request.getEmail(), verificationCode);
            log.info("User registration completed successfully for userId: {}", userId);
            return RegistrationResponse.builder()
                    .userId(userId)
                    .build();
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
    public boolean verifyResetCode(String code, String userId) {
        log.info("Verifying password reset code for userId: {}", userId);
        String cacheKey = PASSWORD_RESET_CODE_PREFIX + userId;
        String cachedCode = redisTemplate.opsForValue().get(cacheKey);
        boolean isValid = code != null && code.equals(cachedCode);
        if (isValid) {
            redisTemplate.expire(cacheKey, 10, TimeUnit.MINUTES);
            log.info("Password reset code validated successfully for userId: {}", userId);
        } else {
            log.warn("Invalid password reset code provided for userId: {}", userId);
        }

        return isValid;
    }

    private String generateAndCacheVerificationCode(String verificationPrefix, String userId) {
        String verificationCode = String.format("%06d", random.nextInt(1000000));
        String cacheKey = verificationPrefix + userId;
        redisTemplate.opsForValue().set(cacheKey, verificationCode, CODE_EXPIRATION_MINUTES, TimeUnit.MINUTES);
        log.info("Generated verification code for userId: {}", userId);
        return verificationCode;
    }

    private void publishVerificationCodeEvent(String userId, String email, String verificationCode) {
        EmailVerificationEvent event = EmailVerificationEvent.builder()
                .email(email)
                .verificationCode(verificationCode)
                .verificationCodeTtl(CODE_EXPIRATION_MINUTES)
                .build();
        kafkaTemplateEmail.send(KafkaConfig.TOPIC_EMAIL_VERIFICATION, userId, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("Published verification code event for userId: {}", userId);
                    } else {
                        log.error("Failed to publish verification code event for userId: {}", userId, ex);
                    }
                });
    }

    private void publishUserRegisteredEvent(String userId, UserRegistrationRequest request) {
        AuthEvent event = AuthEvent.builder()
                .type(AuthEventType.USER_REGISTERED)
                .userId(userId)
                .data(Map.of(
                        "email", request.getEmail(),
                        "firstName", request.getFirstName(),
                        "lastName", request.getLastName()
                ))
                .build();
        publishEvent("auth-events", event, userId);
    }

    @Override
    public void resetPassword(String userId, String code, String newPassword) {
        log.info("Processing password reset for userId: {}", userId);
        if(!verifyResetCode(code, userId)) {
            throw new AuthException(
                    "Invalid or expired reset code",
                    "INVALID_CODE",
                    HttpStatus.BAD_REQUEST
            );
        }

        try {
            keycloakClient.resetPassword(userId, newPassword);
            String cacheKey = PASSWORD_RESET_CODE_PREFIX + userId;
            redisTemplate.delete(cacheKey);
            log.info("Password reset completed successfully for userId: {}", userId);
        } catch (Exception e) {
            log.error("Password reset failed for userId: {}", userId, e);
            throw new AuthException(
                    "Failed to reset password",
                    "RESET_FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e
            );
        }
    }

    @Override
    public void deleteAccount(String userId) {
        log.info("Processing account deletion for userId: {}", userId);
        try {
            String email = keycloakClient.getEmailByUserId(userId);
            publishAccountDeletionEvent(userId, email);
            keycloakClient.deleteUser(userId);
            log.info("User account deleted successfully for userId: {}", userId);
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Account deletion failed for userId: {}", userId, e);
            throw new AuthException(
                    "Failed to delete account",
                    "DELETE_FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e
            );
        }
    }

    private void publishAccountDeletionEvent(String userId, String email) {
        AuthEvent event = AuthEvent.builder()
                .type(AuthEventType.USER_DELETED)
                .userId(userId)
                .data(Map.of(
                        "email", email,
                        "deletedAt", System.currentTimeMillis()
                ))
                .build();
        kafkaTemplate.send(KafkaConfig.TOPIC_AUTH_EVENTS, userId, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published account deletion event for userId: {}", userId);
                    } else {
                        log.error("Failed to publish account deletion event for userId: {}", userId, ex);
                    }
                });
    }

    @Override
    public boolean validateTokenBelongsToUser(String token, String userId) {
        try {
            if (!keycloakClient.verifyToken(token)) {
                return false;
            }
            String tokenUserId = keycloakClient.getUserIdFromToken(token);
            return userId.equals(tokenUserId);
        } catch (Exception e) {
            log.error("Error validating token ownership", e);
            return false;
        }
    }

    private void publishEvent(String topic, Object event, String key) {
        genericKafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("Published event to topic {} with key {}", topic, key);
                    } else {
                        log.error("Failed to publish event to topic {} with key {}", topic, key, ex);
                    }
                });
    }

    private void publishPasswordResetInitiatedEvent(String userId, String email, String resetCode) {
        PasswordResetEvent event = PasswordResetEvent.builder()
                .email(email)
                .resetCode(resetCode)
                .resetCodeTtl(CODE_EXPIRATION_MINUTES)
                .build();
        kafkaTemplatePasswordReset.send(KafkaConfig.TOPIC_PASSWORD_RESET, userId, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("Published password reset event for userId: {}", userId);
                    } else {
                        log.error("Failed to publish password reset event for userId: {}", userId, ex);
                    }
                });
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

    private void validateUniqueEmail(String email) {
        if (keycloakClient.emailExists(email)) {
            throw new AuthException(
                    "Email already registered",
                    "EMAIL_EXISTS",
                    HttpStatus.CONFLICT
            );
        }
    }

    public boolean isEmailVerified(String email) {
        String userId = keycloakClient.getUserIdByEmail(email);
        boolean verified = keycloakClient.isEmailVerified(userId);
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
    public void verifyEmail(String code, String userId) {
        log.info("Processing email verification with code for userId: {}", userId);
        try {
            if (!verifyEmailCode(userId, code)) {
                throw new AuthException(
                        "Invalid or expired verification code",
                        "INVALID_CODE",
                        HttpStatus.BAD_REQUEST
                );
            }
            keycloakClient.updateEmailVerificationStatus(userId, true);
            log.info("Email verified successfully for userId: {}", userId);
        } catch (AuthException e) {
            throw e;
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

    private boolean verifyEmailCode(String userId, String code) {
        String cacheKey = EMAIL_VERIFICATION_CODE_PREFIX + userId;
        String cachedCode = redisTemplate.opsForValue().get(cacheKey);
        boolean isValid = code != null && code.equals(cachedCode);
        if (isValid) {
            redisTemplate.delete(cacheKey);
            log.info("Email verification code validated successfully for userId: {}", userId);
        } else {
            log.warn("Invalid verification code provided for userId: {}", userId);
        }

        return isValid;
    }

    @Override
    public String initiatePasswordReset(String email) {
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
            String resetCode = generateAndCacheVerificationCode(PASSWORD_RESET_CODE_PREFIX, userId);
            publishPasswordResetInitiatedEvent(userId, email, resetCode);
            log.info("Password reset initiated successfully for userId: {}", userId);
            return userId;
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
}