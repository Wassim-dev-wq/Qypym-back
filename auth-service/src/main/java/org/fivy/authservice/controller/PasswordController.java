package org.fivy.authservice.controller;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.fivy.authservice.dto.ForgotPasswordRequest;
import org.fivy.authservice.dto.ResetPasswordRequest;
import org.fivy.authservice.service.KeycloakAdminClientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/password")
public class PasswordController {
    private final KeycloakAdminClientService keycloakService;


    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        keycloakService.resetPassword(request.getUserId(), request.getNewPassword());
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PostMapping("/forgot")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        keycloakService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(HttpStatus.OK);
    }

}
