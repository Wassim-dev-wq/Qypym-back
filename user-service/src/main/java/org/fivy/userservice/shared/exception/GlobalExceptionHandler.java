package org.fivy.userservice.shared.exception;

import org.fivy.userservice.api.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserException(
            UserException ex) {
        org.fivy.userservice.api.dto.ApiResponse<Object> response =
                org.fivy.userservice.api.dto.ApiResponse.error(
                        ex.getMessage(),
                        ex.getErrorCode()
                );
        return new ResponseEntity<>(response, ex.getStatus());
    }
}