package org.fivy.matchservice.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class MatchException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;

    public MatchException(String message, String errorCode, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
}
