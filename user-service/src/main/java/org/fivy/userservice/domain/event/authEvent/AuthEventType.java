package org.fivy.userservice.domain.event.authEvent;

import lombok.Getter;

@Getter
public enum AuthEventType {
    USER_REGISTERED("user.registered"),
    USER_UPDATED("user.updated"),
    USER_DELETED("user.deleted"),
    USER_LOGGED_IN("user.logged_in"),
    USER_LOGGED_OUT("user.logged_out"),
    EMAIL_VERIFIED("user.email_verified"),
    PASSWORD_RESET_REQUESTED("user.password_reset_requested"),
    PASSWORD_RESET_COMPLETED("user.password_reset_completed");

    private final String eventName;

    AuthEventType(String eventName) {
        this.eventName = eventName;
    }
}