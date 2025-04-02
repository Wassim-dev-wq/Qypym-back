package org.fivy.notificationservice.shared;

import org.fivy.notificationservice.shared.exception.NotificationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

public class JwtConverter {
    public static UUID extractUserIdFromJwt(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new NotificationException(
                    "JWT authentication required",
                    "INVALID_AUTHENTICATION",
                    HttpStatus.UNAUTHORIZED
            );
        }
        Jwt jwt = jwtAuth.getToken();
        String userId = jwt.getSubject();
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new NotificationException(
                    "Invalid user ID format in token",
                    "INVALID_USER_ID",
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
