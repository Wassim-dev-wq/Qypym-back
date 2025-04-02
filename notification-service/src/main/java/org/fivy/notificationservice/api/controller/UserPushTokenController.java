package org.fivy.notificationservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.notificationservice.api.dto.request.UserPushTokenRequest;
import org.fivy.notificationservice.api.dto.response.UserPushTokenResponse;
import org.fivy.notificationservice.application.service.UserPushTokenService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.fivy.notificationservice.shared.JwtConverter.extractUserIdFromJwt;

@RestController
@RequestMapping("/api/v1/user-push-tokens")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearer-auth")
@PreAuthorize("isAuthenticated()")
public class UserPushTokenController {

    private final UserPushTokenService userPushTokenService;

    @PostMapping("/register")
    @Operation(summary = "Register a push token", description = "Registers an Expo push token for a user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public org.fivy.notificationservice.api.dto.ApiResponse<UserPushTokenResponse> registerToken(
            Authentication authentication,
            @RequestBody UserPushTokenRequest request
    ) {
        UUID userId = extractUserIdFromJwt(authentication);
        request.setUserId(userId);
        log.info("Registering push token for user: {}", userId);
        UserPushTokenResponse response = userPushTokenService.registerToken(request);
        return org.fivy.notificationservice.api.dto.ApiResponse.success(response);
    }

    @GetMapping("")
    @Operation(summary = "Get push tokens", description = "Retrieves all push tokens for the current user")
    public org.fivy.notificationservice.api.dto.ApiResponse<List<UserPushTokenResponse>> getTokens(
            Authentication authentication
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("Fetching push tokens for user: {}", currentUserId);
        List<UserPushTokenResponse> tokens = userPushTokenService.getTokensByUserId(currentUserId);
        return org.fivy.notificationservice.api.dto.ApiResponse.success(tokens);
    }

    @DeleteMapping("")
    @Operation(summary = "Delete a push token", description = "Deletes a specific push token for the current user")
    public org.fivy.notificationservice.api.dto.ApiResponse<Void> deleteToken(
            Authentication authentication,
            @RequestParam String expoToken
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("Deleting push token for user: {}", currentUserId);
        userPushTokenService.deleteToken(currentUserId, expoToken);
        return org.fivy.notificationservice.api.dto.ApiResponse.success(null);
    }
}
