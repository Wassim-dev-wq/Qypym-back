package org.fivy.authservice.service.Impl;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.fivy.authservice.dto.LoginRequest;
import org.fivy.authservice.dto.TokenResponse;
import org.fivy.authservice.dto.UserRegistrationRequest;
import org.fivy.authservice.service.KeycloakAdminClientService;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KeycloakAdminClientServiceImpl implements KeycloakAdminClientService {

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.auth-server-url}")
    private final String authServerUrl;


    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    @Override
    public void registerUser(UserRegistrationRequest request) {
        Keycloak keycloak = getKeycloakInstance();

        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true);
        user.setEmailVerified(false);

        Response response = keycloak.realm(realm).users().create(user);
        if (response.getStatus() != 201) {
            throw new RuntimeException("Failed to create user");
        }

        String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

        CredentialRepresentation passwordCred = new CredentialRepresentation();
        passwordCred.setTemporary(false);
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue(request.getPassword());

        keycloak.realm(realm).users().get(userId).resetPassword(passwordCred);

        UserResource userResource = keycloak.realm(realm).users().get(userId);
        UserRepresentation userRepresentation = userResource.toRepresentation();
        userRepresentation.setEnabled(true);
        userResource.update(userRepresentation);
        keycloak.realm(realm).users().get(userId).sendVerifyEmail();
    }

    public void resendVerificationEmail(String userId) {
        Keycloak keycloak = getKeycloakInstance();
        try {
            keycloak.realm(realm)
                    .users()
                    .get(userId)
                    .sendVerifyEmail();
        } catch (Exception e) {
            throw new RuntimeException("Failed to resend verification email", e);
        }
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        try {
            Keycloak adminClient = getKeycloakInstance();
            List<UserRepresentation> users = adminClient.realm(realm)
                    .users()
                    .search(request.getEmail());

            if (users.isEmpty()) {
                throw new RuntimeException("User not found");
            }
            UserRepresentation user = users.get(0);
            if (!user.isEmailVerified()) {
                throw new RuntimeException("Email not verified");
            }
            Keycloak userClient = KeycloakBuilder.builder()
                    .serverUrl(authServerUrl)
                    .realm(realm)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .username(request.getEmail())
                    .password(request.getPassword())
                    .grantType(OAuth2Constants.PASSWORD)
                    .build();
            AccessTokenResponse response = userClient.tokenManager().getAccessToken();

            return TokenResponse.builder()
                    .accessToken(response.getToken())
                    .refreshToken(response.getRefreshToken())
                    .expiresIn(response.getExpiresIn())
                    .tokenType(response.getTokenType())
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Login failed: " + e.getMessage());
        }
    }

    @Override
    public boolean isEmailVerified(String username) {
        Keycloak keycloak = getKeycloakInstance();
        List<UserRepresentation> users = keycloak.realm(realm)
                .users()
                .search(username);

        if (users.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        return users.get(0).isEmailVerified();
    }

    @Override
    public void logout(String userId, String refreshToken) {
        try {
            Keycloak keycloak = getKeycloakInstance();
            if (userId != null) {
                keycloak.realm(realm)
                        .users()
                        .get(userId)
                        .logout();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to logout user", e);
        }
    }

    @Override
    public void resetPassword(String userId, String newPassword) {
        Keycloak keycloak = getKeycloakInstance();
        CredentialRepresentation passwordCred = new CredentialRepresentation();
        passwordCred.setTemporary(false);
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue(newPassword);

        keycloak.realm(realm)
                .users()
                .get(userId)
                .resetPassword(passwordCred);
    }

    @Override
    public void forgotPassword(String userEmail) {
        Keycloak keycloak = getKeycloakInstance();
        List<UserRepresentation> users = keycloak.realm(realm)
                .users()
                .search(userEmail);

        if (users.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        keycloak.realm(realm)
                .users()
                .get(users.get(0).getId())
                .executeActionsEmail(List.of("UPDATE_PASSWORD"));
    }

    private Keycloak getKeycloakInstance() {
        return KeycloakBuilder.builder()
                .serverUrl(authServerUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType("client_credentials")
                .build();
    }

}
