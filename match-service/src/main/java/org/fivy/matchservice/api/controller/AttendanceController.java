package org.fivy.matchservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.api.dto.request.CodeVerificationRequest;
import org.fivy.matchservice.api.dto.request.ManualAttendanceRequest;
import org.fivy.matchservice.api.dto.response.MatchAttendanceResponse;
import org.fivy.matchservice.api.dto.response.VerificationCodeResponse;
import org.fivy.matchservice.application.service.AttendanceService;
import org.fivy.matchservice.domain.enums.AttendanceStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.fivy.matchservice.shared.JwtConverter.extractUserIdFromJwt;

@RestController
@RequestMapping("/api/v1/matches/{matchId}/attendance")
@RequiredArgsConstructor
@Slf4j
@Validated
@SecurityRequirement(name = "bearer-auth")
@PreAuthorize("isAuthenticated()")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/code")
    @Operation(summary = "Generate verification code for match attendance",
            description = "Retrieves or generates a time-limited verification code for match attendance confirmation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Code generated successfully"),
            @ApiResponse(responseCode = "404", description = "Match not found")
    })
    public org.fivy.matchservice.api.dto.ApiResponse<VerificationCodeResponse> getMatchVerificationCode(
            @PathVariable UUID matchId,
            Authentication authentication
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("User {} requesting verification code for match: {}", currentUserId, matchId);
        VerificationCodeResponse code = attendanceService.generateVerificationCode(matchId);
        return org.fivy.matchservice.api.dto.ApiResponse.success(code);
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirm attendance via verification code",
            description = "Player confirms attendance by entering the verification code")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attendance confirmed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid verification code"),
            @ApiResponse(responseCode = "404", description = "Match not found")
    })
    public org.fivy.matchservice.api.dto.ApiResponse<MatchAttendanceResponse> confirmAttendance(
            @PathVariable UUID matchId,
            @Valid @RequestBody CodeVerificationRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("User {} confirming attendance via code for match: {}", currentUserId, matchId);
        MatchAttendanceResponse attendance = attendanceService.confirmAttendanceViaCode(
                matchId, request.getVerificationCode(), currentUserId);
        return org.fivy.matchservice.api.dto.ApiResponse.success(attendance);
    }

    @PostMapping("/manual-confirm")
    @Operation(summary = "Manually confirm player attendance",
            description = "Match owner manually confirms a player's attendance")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attendance confirmed successfully"),
            @ApiResponse(responseCode = "403", description = "Only match owner can manually confirm attendance"),
            @ApiResponse(responseCode = "404", description = "Match not found")
    })
    public org.fivy.matchservice.api.dto.ApiResponse<MatchAttendanceResponse> manuallyConfirmAttendance(
            @PathVariable UUID matchId,
            @Valid @RequestBody ManualAttendanceRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("User {} manually confirming attendance for player {} in match: {}",
                currentUserId, request.getPlayerId(), matchId);
        MatchAttendanceResponse attendance = attendanceService.confirmAttendanceManually(
                matchId, request.getPlayerId(), currentUserId);
        return org.fivy.matchservice.api.dto.ApiResponse.success(attendance);
    }

    @GetMapping
    @Operation(summary = "Get match attendance records",
            description = "Retrieves all attendance records for a match")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attendance records retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Match not found")
    })
    public org.fivy.matchservice.api.dto.ApiResponse<List<MatchAttendanceResponse>> getMatchAttendances(
            @PathVariable UUID matchId,
            Authentication authentication
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("User {} fetching attendance records for match: {}", currentUserId, matchId);
        List<MatchAttendanceResponse> attendances = attendanceService.getMatchAttendances(matchId);
        return org.fivy.matchservice.api.dto.ApiResponse.success(attendances);
    }

    @GetMapping("/status")
    @Operation(summary = "Get player attendance status",
            description = "Check if the current player has confirmed attendance")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attendance status retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Match not found")
    })
    public org.fivy.matchservice.api.dto.ApiResponse<AttendanceStatus> getPlayerAttendanceStatus(
            @PathVariable UUID matchId,
            Authentication authentication
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("Checking attendance status for user {} in match: {}", currentUserId, matchId);
        AttendanceStatus status = attendanceService.getPlayerAttendanceStatus(matchId, currentUserId);
        return org.fivy.matchservice.api.dto.ApiResponse.success(status);
    }

    @GetMapping("/count")
    @Operation(summary = "Get confirmed attendance count",
            description = "Retrieves the number of confirmed attendees for a match")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Count retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Match not found")
    })
    public org.fivy.matchservice.api.dto.ApiResponse<Long> getAttendanceCount(
            @PathVariable UUID matchId,
            Authentication authentication
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("User {} fetching attendance count for match: {}", currentUserId, matchId);
        long count = attendanceService.getAttendanceCount(matchId);
        return org.fivy.matchservice.api.dto.ApiResponse.success(count);
    }
}