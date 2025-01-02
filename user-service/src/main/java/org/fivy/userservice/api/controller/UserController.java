package org.fivy.userservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.userservice.api.dto.update.ProfileResponse;
import org.fivy.userservice.api.dto.update.UpdateProfileRequest;
import org.fivy.userservice.api.dto.update.UserProfileResponse;
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
@SecurityRequirement(name = "bearer-auth")
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    @Operation(summary = "Get user profile",
            description = "Retrieves detailed user profile information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public org.fivy.userservice.api.dto.ApiResponse<ProfileResponse> getUserProfile(
            @PathVariable String userId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        MDC.put("correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString());
        log.info("Fetching user profile for userId: {}", userId);

        try {
            ProfileResponse profile = userService.getUserProfile(userId);
            return org.fivy.userservice.api.dto.ApiResponse.success(profile);
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
