package org.fivy.matchservice.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class FeedbackException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;

    public FeedbackException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}