package org.fivy.userservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.userservice.api.dto.UserResponseDTO;
import org.fivy.userservice.application.service.UserService;
import org.fivy.userservice.shared.exception.UserException;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearer-auth")
@PreAuthorize("isAuthenticated()")
@Tag(name = "User Profile Photo API", description = "Endpoints for managing user profile photos")
public class UserProfilePhotoController {

    private final UserService userService;

    @Operation(summary = "Upload profile photo", description = "Upload a profile photo for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Photo uploaded successfully",
            content = @Content(schema = @Schema(implementation = UserResponseDTO.class)))
    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponseDTO> uploadProfilePhoto(
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            Authentication authentication,
            @RequestParam("file") MultipartFile file) throws IOException {
        String correlationIdValue = correlationId != null ? correlationId : UUID.randomUUID().toString();
        MDC.put("correlationId", correlationIdValue);
        try {
            UUID userId = extractUserIdFromJwt(authentication);
            UserResponseDTO response = userService.uploadProfilePhoto(userId, file);
            return ResponseEntity.ok(response);
        } finally {
            MDC.clear();
        }
    }

    @Operation(summary = "Get profile photo", description = "Retrieve the authenticated user's profile photo")
    @ApiResponse(responseCode = "200", description = "Photo retrieved successfully",
            content = @Content(mediaType = "image/*"))
    @GetMapping("/me/photo")
    public ResponseEntity<byte[]> getProfilePhoto(
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            Authentication authentication) {
        String correlationIdValue = correlationId != null ? correlationId : UUID.randomUUID().toString();
        MDC.put("correlationId", correlationIdValue);
        try {
            UUID userId = extractUserIdFromJwt(authentication);
            byte[] photoData = userService.getProfilePhoto(userId);
            UserResponseDTO userResponse = userService.getUserById(userId);
            String contentType = userResponse.getPhotoContentType() != null
                    ? userResponse.getPhotoContentType()
                    : MediaType.IMAGE_JPEG_VALUE;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            return new ResponseEntity<>(photoData, headers, HttpStatus.OK);
        } finally {
            MDC.clear();
        }
    }

    @Operation(summary = "Get user profile photo", description = "Retrieve a specific user's profile photo")
    @ApiResponse(responseCode = "200", description = "Photo retrieved successfully",
            content = @Content(mediaType = "image/*"))
    @GetMapping("/{userId}/photo")
    public ResponseEntity<byte[]> getUserProfilePhoto(
            @PathVariable UUID userId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String correlationIdValue = correlationId != null ? correlationId : UUID.randomUUID().toString();
        MDC.put("correlationId", correlationIdValue);
        try {
            byte[] photoData = userService.getProfilePhoto(userId);
            UserResponseDTO userResponse = userService.getUserById(userId);
            String contentType = userResponse.getPhotoContentType() != null
                    ? userResponse.getPhotoContentType()
                    : MediaType.IMAGE_JPEG_VALUE;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            return new ResponseEntity<>(photoData, headers, HttpStatus.OK);
        } finally {
            MDC.clear();
        }
    }

    @Operation(summary = "Delete profile photo", description = "Delete the authenticated user's profile photo")
    @ApiResponse(responseCode = "200", description = "Photo deleted successfully",
            content = @Content(schema = @Schema(implementation = UserResponseDTO.class)))
    @DeleteMapping("/me/photo")
    public ResponseEntity<UserResponseDTO> deleteProfilePhoto(
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            Authentication authentication) {
        String correlationIdValue = correlationId != null ? correlationId : UUID.randomUUID().toString();
        MDC.put("correlationId", correlationIdValue);
        try {
            UUID userId = extractUserIdFromJwt(authentication);
            UserResponseDTO response = userService.deleteProfilePhoto(userId);
            return ResponseEntity.ok(response);
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
