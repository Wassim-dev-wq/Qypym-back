package org.fivy.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.fivy.authservice.dto.LoginRequest;
import org.fivy.authservice.dto.LogoutRequest;
import org.fivy.authservice.dto.TokenResponse;
import org.fivy.authservice.dto.UserRegistrationRequest;
import org.fivy.authservice.service.KeycloakAdminClientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final KeycloakAdminClientService keycloakService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody UserRegistrationRequest request) {
        keycloakService.registerUser(request);
        return ResponseEntity.ok("User registered successfully. Please verify your email.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest request) {
        if (!keycloakService.isEmailVerified(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Email is not verified. Please verify your email.");
        }

        TokenResponse response = keycloakService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@RequestBody LogoutRequest request) {
        keycloakService.logout(request.getUserId(), request.getRefreshToken());
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PostMapping("/resend-verification/{userId}")
    public ResponseEntity<?> resendVerificationEmail(@PathVariable String userId) {
        keycloakService.resendVerificationEmail(userId);
        return ResponseEntity.ok().build();
    }
}
