package org.fivy.authservice.domain.enums;

import lombok.Getter;

@Getter
public enum AuthEventType {
    VERIFY_EMAIL("user.email_verification"),
    USER_REGISTERED("user.registered"),
    USER_LOGGED_IN("user.logged_in"),
    USER_LOGGED_OUT("user.logged_out"),
    USER_DELETED("user.deleted"),
    EMAIL_VERIFIED("user.email_verified"),
    PASSWORD_RESET_REQUESTED("user.password_reset_requested"),
    PASSWORD_RESET_COMPLETED("user.password_reset_completed");

    private final String eventName;

    AuthEventType(String eventName) {
        this.eventName = eventName;
    }

}