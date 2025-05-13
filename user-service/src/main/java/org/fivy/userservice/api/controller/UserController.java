package org.fivy.userservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.userservice.api.dto.ContentReportDTO;
import org.fivy.userservice.api.dto.UserResponseDTO;
import org.fivy.userservice.api.dto.update.UpdateProfileRequest;
import org.fivy.userservice.application.service.UserService;
import org.fivy.userservice.shared.exception.UserException;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Validated
@SecurityRequirement(name = "bearer-auth")
@PreAuthorize("isAuthenticated()")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(
            summary = "Get current user profile",
            description = "Retrieves the profile of the currently authenticated user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public org.fivy.userservice.api.dto.ApiResponse<UserResponseDTO> getCurrentUserProfile(
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            Authentication authentication
    ) {
        String correlationIdValue = correlationId != null ? correlationId : UUID.randomUUID().toString();
        MDC.put("correlationId", correlationIdValue);

        try {
            UUID userId = extractUserIdFromJwt(authentication);
            log.info("Fetching current user profile for userId from JWT: {}", userId);

            UserResponseDTO user = userService.getUserById(userId);
            return org.fivy.userservice.api.dto.ApiResponse.success(user);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/{userId}")
    @Operation(
            summary = "Get user profile by ID",
            description = "Retrieves detailed user profile information. User can only access their own profile unless they have admin privileges."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to access this profile"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public org.fivy.userservice.api.dto.ApiResponse<UserResponseDTO> getUserProfile(
            @PathVariable String userId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            Authentication authentication
    ) {
        String correlationIdValue = correlationId != null ? correlationId : UUID.randomUUID().toString();
        MDC.put("correlationId", correlationIdValue);

        try {
            log.info("Fetching user profile for userId: {}", userId);

            UserResponseDTO user = userService.getUserById(UUID.fromString(userId));
            return org.fivy.userservice.api.dto.ApiResponse.success(user);
        } finally {
            MDC.clear();
        }
    }

    @PutMapping("/profile")
    @Operation(
            summary = "Update current user profile",
            description = "Updates profile information for the currently authenticated user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public org.fivy.userservice.api.dto.ApiResponse<UserResponseDTO> updateCurrentUserProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            Authentication authentication
    ) {
        String correlationIdValue = correlationId != null ? correlationId : UUID.randomUUID().toString();
        MDC.put("correlationId", correlationIdValue);

        try {
            UUID userId = extractUserIdFromJwt(authentication);
            log.info("Updating profile for current user with ID: {}", userId);
            UserResponseDTO updatedProfile = userService.updateProfile(userId, request);
            return org.fivy.userservice.api.dto.ApiResponse.success(updatedProfile);
        } finally {
            MDC.clear();
        }
    }

    @PutMapping("/{userId}")
    @Operation(
            summary = "Update user profile by ID",
            description = "Updates user profile information for the specified user. Requires admin privileges."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to update this profile"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public org.fivy.userservice.api.dto.ApiResponse<UserResponseDTO> updateUserProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateProfileRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        String correlationIdValue = correlationId != null ? correlationId : UUID.randomUUID().toString();
        MDC.put("correlationId", correlationIdValue);

        try {
            log.info("Admin updating profile for userId: {}", userId);

            UserResponseDTO updatedProfile = userService.updateProfile(userId, request);
            return org.fivy.userservice.api.dto.ApiResponse.success(updatedProfile);
        } finally {
            MDC.clear();
        }
    }

    @DeleteMapping("/me")
    @Operation(
            summary = "Delete current user account",
            description = "Soft-deletes the currently authenticated user's account"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public org.fivy.userservice.api.dto.ApiResponse<Void> deleteCurrentUser(
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            Authentication authentication
    ) {
        String correlationIdValue = correlationId != null ? correlationId : UUID.randomUUID().toString();
        MDC.put("correlationId", correlationIdValue);

        try {
            UUID userId = extractUserIdFromJwt(authentication);
            log.info("Deleting current user with ID: {}", userId);

            userService.deleteUser(userId);
            return org.fivy.userservice.api.dto.ApiResponse.success(null);
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/blocks/{blockedUserId}")
    @Operation(
            summary = "Block a user",
            description = "Blocks a user to prevent interaction"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User blocked successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public org.fivy.userservice.api.dto.ApiResponse<Void> blockUser(
            @PathVariable UUID blockedUserId,
            @RequestParam(required = false) String reason,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            Authentication authentication
    ) {
        String correlationIdValue = correlationId != null ? correlationId : UUID.randomUUID().toString();
        MDC.put("correlationId", correlationIdValue);

        try {
            UUID currentUserId = extractUserIdFromJwt(authentication);
            log.info("User {} blocking user {}", currentUserId, blockedUserId);
            userService.blockUser(currentUserId, blockedUserId, reason);
            return org.fivy.userservice.api.dto.ApiResponse.success(null);
        } finally {
            MDC.clear();
        }
    }

    @DeleteMapping("/blocks/{blockedUserId}")
    @Operation(
            summary = "Unblock a user",
            description = "Unblocks a previously blocked user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User unblocked successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public org.fivy.userservice.api.dto.ApiResponse<Void> unblockUser(
            @PathVariable UUID blockedUserId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            Authentication authentication
    ) {
        String correlationIdValue = correlationId != null ? correlationId : UUID.randomUUID().toString();
        MDC.put("correlationId", correlationIdValue);

        try {
            UUID currentUserId = extractUserIdFromJwt(authentication);
            log.info("User {} unblocking user {}", currentUserId, blockedUserId);
            userService.unblockUser(currentUserId, blockedUserId);
            return org.fivy.userservice.api.dto.ApiResponse.success(null);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/blocks")
    @Operation(
            summary = "Get blocked users",
            description = "Retrieves all users blocked by the current user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Blocked users retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public org.fivy.userservice.api.dto.ApiResponse<List<UserResponseDTO>> getBlockedUsers(
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            Authentication authentication
    ) {
        String correlationIdValue = correlationId != null ? correlationId : UUID.randomUUID().toString();
        MDC.put("correlationId", correlationIdValue);

        try {
            UUID currentUserId = extractUserIdFromJwt(authentication);
            log.info("Fetching blocked users for user: {}", currentUserId);
            List<UserResponseDTO> blockedUsers = userService.getBlockedUsers(currentUserId);
            return org.fivy.userservice.api.dto.ApiResponse.success(blockedUsers);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/blocks/check/{userId}")
    @Operation(
            summary = "Check if user is blocked",
            description = "Checks if there is a blocking relationship between the current user and specified user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Check completed successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public org.fivy.userservice.api.dto.ApiResponse<Boolean> isUserBlocked(
            @PathVariable UUID userId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            Authentication authentication
    ) {
        String correlationIdValue = correlationId != null ? correlationId : UUID.randomUUID().toString();
        MDC.put("correlationId", correlationIdValue);
        try {
            UUID currentUserId = extractUserIdFromJwt(authentication);
            log.info("Checking if user {} is blocked by user {}", userId, currentUserId);
            boolean isBlocked = userService.isUserBlocked(currentUserId, userId);
            return org.fivy.userservice.api.dto.ApiResponse.success(isBlocked);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/blocks/ids")
    @Operation(
            summary = "Get blocked user IDs",
            description = "Retrieves IDs of all users in a blocking relationship with the current user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Blocked user IDs retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public org.fivy.userservice.api.dto.ApiResponse<List<UUID>> getBlockedUserIds(
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            Authentication authentication
    ) {
        String correlationIdValue = correlationId != null ? correlationId : UUID.randomUUID().toString();
        MDC.put("correlationId", correlationIdValue);

        try {
            UUID currentUserId = extractUserIdFromJwt(authentication);
            log.info("Fetching blocked user IDs for user: {}", currentUserId);
            List<UUID> blockedUserIds = userService.getBlockedUserIds(currentUserId);
            return org.fivy.userservice.api.dto.ApiResponse.success(blockedUserIds);
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/reports/{reportedUserId}")
    @Operation(
            summary = "Report a user",
            description = "Reports a user for inappropriate content or behavior"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public org.fivy.userservice.api.dto.ApiResponse<ContentReportDTO> reportUser(
            @PathVariable UUID reportedUserId,
            @RequestParam(required = false) String details,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            Authentication authentication
    ) {
        String correlationIdValue = correlationId != null ? correlationId : UUID.randomUUID().toString();
        MDC.put("correlationId", correlationIdValue);

        try {
            UUID currentUserId = extractUserIdFromJwt(authentication);
            log.info("User {} reporting user {}", currentUserId, reportedUserId);
            ContentReportDTO report = userService.reportUser(currentUserId, reportedUserId, details);
            return org.fivy.userservice.api.dto.ApiResponse.success(report);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/reports")
    @Operation(
            summary = "Get my reports",
            description = "Retrieves reports submitted by the current user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reports retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public org.fivy.userservice.api.dto.ApiResponse<Page<ContentReportDTO>> getMyReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            Authentication authentication
    ) {
        String correlationIdValue = correlationId != null ? correlationId : UUID.randomUUID().toString();
        MDC.put("correlationId", correlationIdValue);

        try {
            UUID currentUserId = extractUserIdFromJwt(authentication);
            log.info("Fetching reports submitted by user: {}", currentUserId);
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<ContentReportDTO> reports = userService.getUserReports(currentUserId, pageable);
            return org.fivy.userservice.api.dto.ApiResponse.success(reports);
        } finally {
            MDC.clear();
        }
    }

    private UUID extractUserIdFromJwt(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new UserException(
                    "JWT authentication required",
                    "INVALID_AUTHENTICATION",
                    HttpStatus.UNAUTHORIZED
            );
        }
        Jwt jwt = jwtAuth.getToken();
        String userId = jwt.getSubject();
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new UserException(
                    "Invalid user ID format in token",
                    "INVALID_USER_ID",
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}