package org.fivy.matchservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.api.dto.request.UserPushTokenRequest;
import org.fivy.matchservice.api.dto.response.UserPushTokenResponse;
import org.fivy.matchservice.application.service.UserPushTokenService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user-push-tokens")
@RequiredArgsConstructor
@Slf4j
public class UserPushTokenController {

    private final UserPushTokenService userPushTokenService;

    @PostMapping("/register")
    @Operation(summary = "Register a push token", description = "Registers an Expo push token for a user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public org.fivy.matchservice.api.dto.ApiResponse<UserPushTokenResponse> registerToken(@RequestHeader("X-User-ID") String userId, @RequestBody UserPushTokenRequest request) {
        request.setUserId(UUID.fromString(userId));
        log.info("Registering push token for user: {}", request.getUserId());
        UserPushTokenResponse response = userPushTokenService.registerToken(request);
        return org.fivy.matchservice.api.dto.ApiResponse.success(response);
    }

    @GetMapping("")
    @Operation(summary = "Get push tokens", description = "Retrieves all push tokens for the current user")
    public org.fivy.matchservice.api.dto.ApiResponse<List<UserPushTokenResponse>> getTokens(@RequestHeader("X-User-ID") String userId) {
        UUID currentUserId = UUID.fromString(userId);
        List<UserPushTokenResponse> tokens = userPushTokenService.getTokensByUserId(currentUserId);
        return org.fivy.matchservice.api.dto.ApiResponse.success(tokens);
    }

    @DeleteMapping("")
    @Operation(summary = "Delete a push token", description = "Deletes a specific push token for the current user")
    public org.fivy.matchservice.api.dto.ApiResponse<Void> deleteToken(
            @RequestHeader("X-User-ID") String userId,
            @RequestParam String expoToken
    ) {
        UUID currentUserId = UUID.fromString(userId);
        userPushTokenService.deleteToken(currentUserId, expoToken);
        return org.fivy.matchservice.api.dto.ApiResponse.success(null);
    }
}
