package org.fivy.matchservice.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InvalidMatchStateException extends MatchException {
    public InvalidMatchStateException(String message) {
        super(
                message,
                "INVALID_MATCH_STATE",
                HttpStatus.BAD_REQUEST
        );
    }
}
