package org.fivy.userservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.userservice.api.dto.UserResponseDTO;
import org.fivy.userservice.api.dto.update.ProfileResponse;
import org.fivy.userservice.api.dto.update.UpdateProfileRequest;
import org.fivy.userservice.application.service.UserService;
import org.slf4j.MDC;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Validated
//@SecurityRequirement(name = "bearer-auth")
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    @Operation(summary = "Get user profile",
            description = "Retrieves detailed user profile information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public org.fivy.userservice.api.dto.ApiResponse<UserResponseDTO> getUserProfile(
            @PathVariable String userId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        MDC.put("correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString());
        log.info("Fetching user profile for userId: {}", userId);

        try {
            UserResponseDTO user = userService.getUserById(UUID.fromString(userId));
            return org.fivy.userservice.api.dto.ApiResponse.success(user);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile",
            description = "Retrieves the profile of the currently authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public org.fivy.userservice.api.dto.ApiResponse<UserResponseDTO> getCurrentUserProfile(
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        MDC.put("correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString());
        log.info("Fetching current user profile for userId: {}", userId);

        try {
            UserResponseDTO user = userService.getUserById(UUID.fromString(userId));
            return org.fivy.userservice.api.dto.ApiResponse.success(user);
        } finally {
            MDC.clear();
        }
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update user profile",
            description = "Updates user profile information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public org.fivy.userservice.api.dto.ApiResponse<ProfileResponse> updateUserProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        log.info("Updating profile for userId: {}", userId);
        ProfileResponse updatedProfile = userService.updateProfile(userId, request);
        return org.fivy.userservice.api.dto.ApiResponse.success(updatedProfile);
    }

}
