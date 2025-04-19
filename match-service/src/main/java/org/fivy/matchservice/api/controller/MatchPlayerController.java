package org.fivy.matchservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.api.dto.request.MatchPlayerRequest;
import org.fivy.matchservice.api.dto.response.MatchPlayerResponse;
import org.fivy.matchservice.application.service.MatchPlayerService;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.fivy.matchservice.shared.JwtConverter.extractUserIdFromJwt;

@RestController
@RequestMapping("/api/v1/player")
@RequiredArgsConstructor
@Slf4j
@Validated
@SecurityRequirement(name = "bearer-auth")
@PreAuthorize("isAuthenticated()")
public class MatchPlayerController {

    private final MatchPlayerService matchPlayerService;

    @PostMapping("/{playerId}/status")
    public org.fivy.matchservice.api.dto.ApiResponse<Void> updatePlayerStatus(
            @PathVariable UUID playerId,
            @Valid @RequestBody MatchPlayerRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("Updating player status: {} for user: {}", playerId, currentUserId);
        matchPlayerService.updatePlayerStatus(playerId, currentUserId, request.getStatus());
        return org.fivy.matchservice.api.dto.ApiResponse.success(null);
    }

    @GetMapping("/{playerId}/status")
    public org.fivy.matchservice.api.dto.ApiResponse<MatchPlayerResponse> getPlayerStatus(
            @PathVariable UUID playerId,
            Authentication authentication
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("Getting player status: {} for user: {}", playerId, currentUserId);
        MatchPlayerResponse response = matchPlayerService.getPlayerStatus(playerId, currentUserId);
        return org.fivy.matchservice.api.dto.ApiResponse.success(response);
    }


    @GetMapping("/status")
    public org.fivy.matchservice.api.dto.ApiResponse<List<MatchPlayerResponse>> getPlayersStatus(
            Authentication authentication
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("Getting players status for user: {}", currentUserId);
        List<MatchPlayerResponse> response = matchPlayerService.getPlayersStatus(currentUserId);
        return org.fivy.matchservice.api.dto.ApiResponse.success(response);
    }

    @DeleteMapping("/{matchId}/{requestId}/leave")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Leave a match")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully left the match"),
            @ApiResponse(responseCode = "404", description = "Match not found")
    })
    public org.fivy.matchservice.api.dto.ApiResponse<Void> leaveMatch(
            @PathVariable UUID matchId,
            @PathVariable UUID requestId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            Authentication authentication
    ) {
        MDC.put("correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString());
        try {
            log.info("Leaving match: {}", matchId);
            UUID userId = extractUserIdFromJwt(authentication);
            matchPlayerService.leaveMatch(requestId, matchId, userId);
            return org.fivy.matchservice.api.dto.ApiResponse.success(null);
        } finally {
            MDC.clear();
        }
    }
}