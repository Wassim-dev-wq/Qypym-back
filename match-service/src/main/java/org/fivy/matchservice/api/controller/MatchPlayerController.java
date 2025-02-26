package org.fivy.matchservice.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.api.dto.ApiResponse;
import org.fivy.matchservice.api.dto.request.MatchPlayerRequest;
import org.fivy.matchservice.api.dto.response.MatchPlayerResponse;
import org.fivy.matchservice.application.service.MatchPlayerService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/player")
@RequiredArgsConstructor
@Slf4j
@Validated
public class MatchPlayerController {

    private final MatchPlayerService matchPlayerService;

    @PostMapping("/{playerId}/status")
    public org.fivy.matchservice.api.dto.ApiResponse<Void> updatePlayerStatus(
            @PathVariable UUID playerId,
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody MatchPlayerRequest request
    ) {
        log.info("Updating player status: {} for user: {}", playerId, userId);
        UUID currentUserId = UUID.fromString(userId);
        matchPlayerService.updatePlayerStatus(playerId, currentUserId, request.getStatus());
        return org.fivy.matchservice.api.dto.ApiResponse.success(null);
    }

    @GetMapping("/{playerId}/status")
    public ApiResponse<MatchPlayerResponse> getPlayerStatus(
            @PathVariable UUID playerId,
            @RequestHeader("X-User-ID") String userId
    ) {
        log.info("Getting player status: {} for user: {}", playerId, userId);
        UUID currentUserId = UUID.fromString(userId);
        MatchPlayerResponse response = matchPlayerService.getPlayerStatus(playerId, currentUserId);
        return org.fivy.matchservice.api.dto.ApiResponse.success(response);
    }

    @GetMapping("/status")
    public ApiResponse<List<MatchPlayerResponse>> getPlayersStatus(
            @RequestHeader("X-User-ID") String userId
    ) {
        log.info("Getting players status for user: {}", userId);
        UUID currentUserId = UUID.fromString(userId);
        List<MatchPlayerResponse> response = matchPlayerService.getPlayersStatus(currentUserId);
        return org.fivy.matchservice.api.dto.ApiResponse.success(response);
    }
}
