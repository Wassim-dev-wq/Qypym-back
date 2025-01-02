package org.fivy.authservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.authservice.api.dto.*;
import org.fivy.authservice.application.service.KeycloakAdminClientService;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AuthController {
    private final KeycloakAdminClientService keycloakService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user",
            description = "Creates a new user account and sends verification email")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User successfully registered"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    public org.fivy.authservice.api.dto.ApiResponse<RegistrationResponse> registerUser(
            @Valid @RequestBody UserRegistrationRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        MDC.put("correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString());
        log.info("Processing registration request for email: {}", request.getEmail());

        try {
            String userId = keycloakService.registerUser(request);
            return org.fivy.authservice.api.dto.ApiResponse.success(new RegistrationResponse(userId,
                    "User registered successfully. Please verify your email."));
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/login")
    @Operation(summary = "User login",
            description = "Authenticates user and returns access token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully authenticated"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Email not verified")
    })
    public org.fivy.authservice.api.dto.ApiResponse<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        MDC.put("correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString());
        log.info("Processing login request for email: {}", request.getEmail());

        try {
            TokenResponse tokenResponse = keycloakService.login(request);
            return org.fivy.authservice.api.dto.ApiResponse.success(tokenResponse);
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout",
            description = "Invalidates the user's session")
    @SecurityRequirement(name = "bearer-auth")
    public org.fivy.authservice.api.dto.ApiResponse<Void> logout(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody LogoutRequest request
    ) {
        log.info("Processing logout request for userId: {}", request.getUserId());
        keycloakService.logout(request.getUserId(), request.getRefreshToken());
        return org.fivy.authservice.api.dto.ApiResponse.success(null);
    }

    @PostMapping("/verify-email/{token}")
    @Operation(summary = "Verify email",
            description = "Verifies user's email address using token")
    public org.fivy.authservice.api.dto.ApiResponse<Void> verifyEmail(@PathVariable String token) {
        log.info("Processing email verification request");
        keycloakService.verifyEmail(token);
        return org.fivy.authservice.api.dto.ApiResponse.success(null);
    }

    @PostMapping("/resend-verification/{userId}")
    @Operation(summary = "Resend verification email",
            description = "Resends the email verification link")
    public org.fivy.authservice.api.dto.ApiResponse<Void> resendVerification(@PathVariable String userId) {
        log.info("Resending verification email for userId: {}", userId);
        keycloakService.resendVerificationEmail(userId);
        return org.fivy.authservice.api.dto.ApiResponse.success(null);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password",
            description = "Initiates password reset process")
    public org.fivy.authservice.api.dto.ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Processing forgot password request for email: {}", request.getEmail());
        keycloakService.initiatePasswordReset(request.getEmail());
        return org.fivy.authservice.api.dto.ApiResponse.success(null);
    }
}
