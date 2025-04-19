package org.fivy.authservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.authservice.api.dto.request.*;
import org.fivy.authservice.api.dto.response.RefreshTokenResponse;
import org.fivy.authservice.api.dto.response.RegistrationResponse;
import org.fivy.authservice.api.dto.response.TokenResponse;
import org.fivy.authservice.application.service.KeycloakAdminClientService;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    public org.fivy.authservice.api.dto.response.ApiResponse<RegistrationResponse> registerUser(
            @Valid @RequestBody UserRegistrationRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        MDC.put("correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString());
        log.info("Processing registration request for email: {}", request.getEmail());

        try {
            RegistrationResponse registrationResponse = keycloakService.registerUser(request);
            return org.fivy.authservice.api.dto.response.ApiResponse.success(registrationResponse);
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
    public org.fivy.authservice.api.dto.response.ApiResponse<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        MDC.put("correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString());
        log.info("Processing login request for email: {}", request.getEmail());

        try {
            TokenResponse tokenResponse = keycloakService.login(request);
            return org.fivy.authservice.api.dto.response.ApiResponse.success(tokenResponse);
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout",
            description = "Invalidates the user's session")
    @SecurityRequirement(name = "bearer-auth")
    public org.fivy.authservice.api.dto.response.ApiResponse<Void> logout(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody LogoutRequest request
    ) {
        log.info("Processing logout request for userId: {}", request.getUserId());
        keycloakService.logout(request.getUserId(), request.getRefreshToken());
        return org.fivy.authservice.api.dto.response.ApiResponse.success(null);
    }

    @PostMapping("/refresh-token")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Refresh Access Token",
            description = "Generates a new access token using a valid refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully refreshed token"),
            @ApiResponse(responseCode = "400", description = "Invalid refresh token"),
            @ApiResponse(responseCode = "401", description = "Refresh token expired or invalid")
    })
    public org.fivy.authservice.api.dto.response.ApiResponse<RefreshTokenResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        MDC.put("correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString());
        log.info("Processing refresh token request");

        try {
            RefreshTokenResponse response = keycloakService.refreshToken(request.getRefreshToken());
            return org.fivy.authservice.api.dto.response.ApiResponse.success(response);
        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to refresh token");
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/verify-token")
    @Operation(summary = "Verify access token",
            description = "Verifies if the provided access token is valid")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token is valid"),
            @ApiResponse(responseCode = "401", description = "Token is invalid or expired")
    })
    public org.fivy.authservice.api.dto.response.ApiResponse<Boolean> verifyToken(
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        MDC.put("correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString());
        log.info("Processing token verification request");
        try {
            boolean isValid = keycloakService.verifyToken(token);
            return org.fivy.authservice.api.dto.response.ApiResponse.success(isValid);
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email with code",
            description = "Verifies user's email address using the verification code")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired verification code"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public org.fivy.authservice.api.dto.response.ApiResponse<Void> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {
        log.info("Processing email verification request for userId: {}", request.getUserId());
        keycloakService.verifyEmail(request.getCode(), request.getUserId());
        return org.fivy.authservice.api.dto.response.ApiResponse.success(null);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password",
            description = "Initiates password reset process")
    @ResponseStatus(HttpStatus.OK)
    public org.fivy.authservice.api.dto.response.ApiResponse<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Processing forgot password request for email: {}", request.getEmail());
        String userId = keycloakService.initiatePasswordReset(request.getEmail());
        return org.fivy.authservice.api.dto.response.ApiResponse.success(userId);
    }

    @PostMapping("/verify-reset-code")
    @Operation(summary = "Verify password reset code",
            description = "Verifies the code sent during password reset process")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reset code verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired reset code"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public org.fivy.authservice.api.dto.response.ApiResponse<Boolean> verifyResetCode(
            @Valid @RequestBody VerifyResetCodeRequest request) {
        log.info("Processing reset code verification for userId: {}", request.getUserId());
        boolean isValid = keycloakService.verifyResetCode(request.getCode(), request.getUserId());
        return org.fivy.authservice.api.dto.response.ApiResponse.success(isValid);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password",
            description = "Resets the password using the verified reset code")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid reset code or password requirements not met"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public org.fivy.authservice.api.dto.response.ApiResponse<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        log.info("Processing password reset for userId: {}", request.getUserId());
        keycloakService.resetPassword(request.getUserId(), request.getCode(), request.getNewPassword());
        return org.fivy.authservice.api.dto.response.ApiResponse.success(null);
    }

    @DeleteMapping("/delete-account")
    @Operation(summary = "Delete user account",
            description = "Permanently deletes a user account and all associated data")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid credentials"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "bearer-auth")
    public org.fivy.authservice.api.dto.response.ApiResponse<Void> deleteAccount(
            @Valid @RequestBody DeleteAccountRequest request) {
        log.info("Processing account deletion request for userId: {}", request.getUserId());
        if (!keycloakService.validateTokenBelongsToUser(request.getToken(), request.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized access to this account");
        }
        keycloakService.deleteAccount(request.getUserId());
        return org.fivy.authservice.api.dto.response.ApiResponse.success(null);
    }
}