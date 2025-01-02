package org.fivy.authservice.infrastructure.client;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.authservice.api.dto.LoginRequest;
import org.fivy.authservice.api.dto.TokenResponse;
import org.fivy.authservice.api.dto.UserRegistrationRequest;
import org.fivy.authservice.infrastructure.config.KeycloakProperties;
import org.fivy.authservice.shared.exception.AuthException;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

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
            user.setUsername(request.getUsername());
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