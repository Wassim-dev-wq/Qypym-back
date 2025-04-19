package org.fivy.authservice.infrastructure.client;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.authservice.api.dto.request.LoginRequest;
import org.fivy.authservice.api.dto.request.UserRegistrationRequest;
import org.fivy.authservice.api.dto.response.RefreshTokenResponse;
import org.fivy.authservice.api.dto.response.TokenResponse;
import org.fivy.authservice.infrastructure.config.KeycloakProperties;
import org.fivy.authservice.shared.exception.AuthException;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakClient {
    private final Keycloak keycloak;
    private final KeycloakProperties properties;
    private final RestTemplate keycloakRestTemplate;

    public boolean emailExists(String email) {
        try {
            return !keycloak.realm(properties.getRealm()).users().searchByEmail(email, true).isEmpty();
        } catch (Exception e) {
            log.error("Error checking email existence", e);
            throw new AuthException("Error checking email existence", "KEYCLOAK_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    public void resetPassword(String userId, String newPassword) {
        try {
            CredentialRepresentation passwordCred = new CredentialRepresentation();
            passwordCred.setType(CredentialRepresentation.PASSWORD);
            passwordCred.setValue(newPassword);
            passwordCred.setTemporary(false);

            keycloak.realm(properties.getRealm()).users().get(userId).resetPassword(passwordCred);
            log.info("Password reset successfully for userId: {}", userId);
        } catch (Exception e) {
            log.error("Error resetting password", e);
            throw new AuthException("Error resetting password", "PASSWORD_RESET_ERROR",
                    HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }
    public String getUserIdByEmail(String email) {
        try {
            List<UserRepresentation> users = keycloak.realm(properties.getRealm()).users().searchByEmail(email, true);
            if (users.isEmpty()) {
                throw new AuthException("User not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND);
            }
            return users.get(0).getId();
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting user ID by email", e);
            throw new AuthException("Error getting user details", "KEYCLOAK_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    public String getUserIdFromToken(String bearerToken) {
        try {
            if (!bearerToken.startsWith("Bearer ")) {
                throw new AuthException("Invalid token format", "INVALID_TOKEN_FORMAT", HttpStatus.UNAUTHORIZED);
            }
            String token = bearerToken.substring(7);
            MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
            parameters.add("token", token);
            parameters.add("client_id", properties.getClientId());
            parameters.add("client_secret", properties.getClientSecret());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(parameters, headers);
            String introspectionUrl = String.format("%s/realms/%s/protocol/openid-connect/token/introspect",
                    properties.getAuthServerUrl(), properties.getRealm());

            ResponseEntity<Map> response = keycloakRestTemplate.postForEntity(
                    introspectionUrl, requestEntity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("sub")) {
                return (String) response.getBody().get("sub");
            }

            throw new AuthException("Could not extract user ID from token", "INVALID_TOKEN", HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            log.error("Error extracting user ID from token", e);
            throw new AuthException("Token validation failed", "TOKEN_VALIDATION_FAILED",
                    HttpStatus.UNAUTHORIZED, e);
        }
    }

    public boolean isEmailVerified(String userId) {
        try {
            UserRepresentation user = keycloak.realm(properties.getRealm()).users().get(userId).toRepresentation();
            return user.isEmailVerified();
        } catch (Exception e) {
            log.error("Error checking email verification status", e);
            throw new AuthException("Error checking email verification", "KEYCLOAK_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    public void deleteUser(String userId) {
        try {
            keycloak.realm(properties.getRealm()).users().get(userId).remove();
        } catch (Exception e) {
            log.error("Error deleting user", e);
            throw new AuthException("Error deleting user", "KEYCLOAK_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    public String getEmailByUserId(String userId) {
        try {
            UserRepresentation user = keycloak.realm(properties.getRealm()).users().get(userId).toRepresentation();
            return user.getEmail();
        } catch (Exception e) {
            log.error("Error getting email by user ID", e);
            throw new AuthException("Error getting email", "KEYCLOAK_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }
    public void logout(String userId, String refreshToken) {
        try {
            keycloak.realm(properties.getRealm()).users().get(userId).logout();
            if (refreshToken != null) {
                keycloak.realm(properties.getRealm()).users().get(userId).revokeConsent("account");
            }
        } catch (Exception e) {
            log.error("Error during logout", e);
            throw new AuthException("Error during logout", "KEYCLOAK_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    public String validateEmailToken(String token) {
        try {
            return parseUserIdFromToken(token);
        } catch (Exception e) {
            log.error("Error validating email token", e);
            throw new AuthException("Invalid or expired verification token", "INVALID_TOKEN", HttpStatus.BAD_REQUEST, e);
        }
    }

    public void updateEmailVerificationStatus(String userId, boolean verified) {
        try {
            UserRepresentation user = keycloak.realm(properties.getRealm()).users().get(userId).toRepresentation();
            user.setEmailVerified(verified);
            keycloak.realm(properties.getRealm()).users().get(userId).update(user);
        } catch (Exception e) {
            log.error("Error updating email verification status", e);
            throw new AuthException("Error updating verification status", "KEYCLOAK_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    public void sendPasswordResetEmail(String userId) {
        try {
            keycloak.realm(properties.getRealm()).users().get(userId).executeActionsEmail(List.of("UPDATE_PASSWORD"));
        } catch (Exception e) {
            log.error("Error sending password reset email", e);
            throw new AuthException("Error sending password reset email", "EMAIL_SEND_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    private String parseUserIdFromToken(String token) {
        throw new UnsupportedOperationException("Token validation not implemented");
    }

    public String createUser(UserRegistrationRequest request) {
        try {
            UserRepresentation user = new UserRepresentation();
            user.setEmail(request.getEmail());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEnabled(true);
            user.setEmailVerified(false);
            Response response = keycloak.realm(properties.getRealm()).users().create(user);
            if (response.getStatus() != 201) {
                throw new AuthException("Failed to create user in Keycloak", "KEYCLOAK_CREATE_FAILED", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
            setUserPassword(userId, request.getPassword());
            return userId;

        } catch (Exception e) {
            log.error("Error creating user in Keycloak", e);
            throw new AuthException("Failed to create user: " + e.getMessage(), "KEYCLOAK_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    public boolean verifyToken(String bearerToken) {
        try {
            if (!bearerToken.startsWith("Bearer ")) {
                throw new AuthException("Invalid token format", "INVALID_TOKEN_FORMAT", HttpStatus.UNAUTHORIZED);
            }
            String token = bearerToken.substring(7);
            MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
            parameters.add("token", token);
            parameters.add("client_id", properties.getClientId());
            parameters.add("client_secret", properties.getClientSecret());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(parameters, headers);
            String introspectionUrl = String.format("%s/realms/%s/protocol/openid-connect/token/introspect", properties.getAuthServerUrl(), properties.getRealm());
            ResponseEntity<Map> response = keycloakRestTemplate.postForEntity(introspectionUrl, requestEntity, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("active")) {
                return Boolean.TRUE.equals(response.getBody().get("active"));
            }
            return false;
        } catch (RestClientException e) {
            log.error("Failed to verify token", e);
            throw new AuthException("Token verification failed", "TOKEN_VERIFICATION_FAILED", HttpStatus.UNAUTHORIZED, e);
        } catch (Exception e) {
            log.error("Unexpected error during token verification", e);
            throw new AuthException("Token verification failed", "TOKEN_VERIFICATION_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    public RefreshTokenResponse refreshToken(String refreshToken) {
        log.debug("Initiating token refresh");

        try {
            MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
            parameters.add("grant_type", "refresh_token");
            parameters.add("client_id", properties.getClientId());
            parameters.add("client_secret", properties.getClientSecret());
            parameters.add("refresh_token", refreshToken);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> requestEntity =
                    new HttpEntity<>(parameters, headers);
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token",
                    properties.getAuthServerUrl(),
                    properties.getRealm());
            ResponseEntity<AccessTokenResponse> response = keycloakRestTemplate.postForEntity(
                    tokenUrl,
                    requestEntity,
                    AccessTokenResponse.class
            );
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new AuthException(
                        "Invalid refresh token response",
                        "INVALID_REFRESH_TOKEN",
                        HttpStatus.UNAUTHORIZED
                );
            }
            AccessTokenResponse tokenResponse = response.getBody();
            return RefreshTokenResponse.builder()
                    .accessToken(tokenResponse.getToken())
                    .refreshToken(tokenResponse.getRefreshToken())
                    .expiresIn(tokenResponse.getExpiresIn())
                    .build();

        } catch (RestClientException e) {
            log.error("Failed to refresh token", e);
            throw new AuthException(
                    "Token refresh failed",
                    "TOKEN_REFRESH_FAILED",
                    HttpStatus.UNAUTHORIZED,
                    e
            );
        } catch (Exception e) {
            log.error("Unexpected error during token refresh", e);
            throw new AuthException(
                    "Token refresh failed",
                    "TOKEN_REFRESH_FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e
            );
        }
    }

    private void setUserPassword(String userId, String password) {
        CredentialRepresentation passwordCred = new CredentialRepresentation();
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue(password);
        passwordCred.setTemporary(false);
        keycloak.realm(properties.getRealm()).users().get(userId).resetPassword(passwordCred);
    }

    public void sendVerificationEmail(String userId) {
        try {
            keycloak.realm(properties.getRealm()).users().get(userId).sendVerifyEmail();
        } catch (Exception e) {
            log.error("Error sending verification email", e);
            throw new AuthException("Failed to send verification email", "EMAIL_SEND_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    public TokenResponse getToken(LoginRequest request) {
        try {
            Keycloak userKeycloak = KeycloakBuilder.builder().serverUrl(properties.getAuthServerUrl()).realm(properties.getRealm()).clientId(properties.getClientId()).clientSecret(properties.getClientSecret()).username(request.getEmail()).password(request.getPassword()).grantType(OAuth2Constants.PASSWORD).build();

            AccessTokenResponse response = userKeycloak.tokenManager().getAccessToken();
            return TokenResponse.builder().accessToken(response.getToken()).refreshToken(response.getRefreshToken()).expiresIn(response.getExpiresIn()).tokenType(response.getTokenType()).build();

        } catch (Exception e) {
            log.error("Error getting token from Keycloak", e);
            throw new AuthException("Authentication failed", "AUTH_FAILED", HttpStatus.UNAUTHORIZED, e);
        }
    }
}