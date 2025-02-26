package org.fivy.matchservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.api.dto.response.MatchResponse;
import org.fivy.matchservice.application.service.MatchSavedService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
@Slf4j
@Validated
public class MatchSavedController {

    private final MatchSavedService matchService;

    @PostMapping("/{matchId}/save")
    @Operation(summary = "Save a match",
            description = "Saves a match for the current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Match saved successfully"),
            @ApiResponse(responseCode = "404", description = "Match not found"),
            @ApiResponse(responseCode = "409", description = "Match already saved")
    })
    public org.fivy.matchservice.api.dto.ApiResponse<Void> saveMatch(
            @PathVariable UUID matchId,
            @RequestHeader("X-User-ID") String userId
    ) {
        log.info("Saving match: {} for user: {}", matchId, userId);
        UUID currentUserId = UUID.fromString(userId);
        matchService.saveMatch(matchId, currentUserId);
        return org.fivy.matchservice.api.dto.ApiResponse.success(null);
    }

    @DeleteMapping("/{matchId}/save")
    @Operation(summary = "Unsave a match",
            description = "Removes a match from user's saved matches")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Match unsaved successfully"),
            @ApiResponse(responseCode = "404", description = "Match not found or not saved")
    })
    public org.fivy.matchservice.api.dto.ApiResponse<Void> unsaveMatch(
            @PathVariable UUID matchId,
            @RequestHeader("X-User-ID") String userId
    ) {
        log.info("Unsaving match: {} for user: {}", matchId, userId);
        UUID currentUserId = UUID.fromString(userId);
        matchService.unsaveMatch(matchId, currentUserId);
        return org.fivy.matchservice.api.dto.ApiResponse.success(null);
    }

    @GetMapping("/saved")
    @Operation(summary = "Get saved matches",
            description = "Retrieves all matches saved by the current user")
    public org.fivy.matchservice.api.dto.ApiResponse<Page<MatchResponse>> getSavedMatches(
            @RequestHeader("X-User-ID") String userId,
            Pageable pageable
    ) {
        log.info("Fetching saved matches for user: {}", userId);
        UUID currentUserId = UUID.fromString(userId);
        Page<MatchResponse> savedMatches = matchService.getSavedMatches(currentUserId, pageable);
        return org.fivy.matchservice.api.dto.ApiResponse.success(savedMatches);
    }

    @GetMapping("/{matchId}/saved")
    @Operation(summary = "Check if match is saved",
            description = "Checks if a match is saved by the current user")
    public org.fivy.matchservice.api.dto.ApiResponse<Boolean> isMatchSaved(
            @PathVariable UUID matchId,
            @RequestHeader("X-User-ID") String userId
    ) {
        log.info("Checking if match: {} is saved for user: {}", matchId, userId);
        UUID currentUserId = UUID.fromString(userId);
        boolean isSaved = matchService.isMatchSaved(matchId, currentUserId);
        return org.fivy.matchservice.api.dto.ApiResponse.success(isSaved);
    }

}
