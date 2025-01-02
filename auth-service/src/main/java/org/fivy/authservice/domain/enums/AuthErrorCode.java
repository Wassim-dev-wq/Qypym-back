package org.fivy.authservice.domain.enums;

import org.fivy.authservice.shared.exception.AuthException;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode {
    USER_NOT_FOUND("AUTH_001", "User not found"),
    INVALID_CREDENTIALS("AUTH_002", "Invalid credentials"),
    EMAIL_NOT_VERIFIED("AUTH_003", "Email not verified"),
    REGISTRATION_FAILED("AUTH_004", "Registration failed"),
    TOKEN_EXPIRED("AUTH_005", "Token expired"),
    INVALID_TOKEN("AUTH_006", "Invalid token");

    private final String code;
    private final String defaultMessage;

    AuthErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public AuthException exception() {
        return new AuthException(defaultMessage, code, HttpStatus.BAD_REQUEST);
    }

    public AuthException exception(String customMessage) {
        return new AuthException(customMessage, code, HttpStatus.BAD_REQUEST);
    }
}