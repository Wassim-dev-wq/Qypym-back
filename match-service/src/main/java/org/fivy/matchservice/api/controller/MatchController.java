package org.fivy.matchservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.api.dto.request.CreateMatchRequest;
import org.fivy.matchservice.api.dto.request.FilterMatchesRequest;
import org.fivy.matchservice.api.dto.request.UpdateMatchRequest;
import org.fivy.matchservice.api.dto.response.MatchDetailsResponse;
import org.fivy.matchservice.api.dto.response.MatchHistoryResponse;
import org.fivy.matchservice.api.dto.response.MatchResponse;
import org.fivy.matchservice.application.service.MatchService;
import org.fivy.matchservice.domain.enums.MatchStatus;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static org.fivy.matchservice.shared.JwtConverter.extractUserIdFromJwt;

@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
@Slf4j
@Validated
@SecurityRequirement(name = "bearer-auth")
@PreAuthorize("isAuthenticated()")
public class MatchController {
    private final MatchService matchService;


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new match",
            description = "Creates a new match with the authenticated user as creator")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Match created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public org.fivy.matchservice.api.dto.ApiResponse<MatchResponse> createMatch(
            @Valid @RequestBody CreateMatchRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            Authentication authentication
    ) {
        MDC.put("correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString());
        log.info("Processing match creation request");
        try {
            UUID creatorId = extractUserIdFromJwt(authentication);
            log.info("Creating match for user: {}", creatorId);
            log.info("Match request: {}", request);
            MatchResponse match = matchService.createMatch(creatorId, request);
            return org.fivy.matchservice.api.dto.ApiResponse.success(match);
        } finally {
            MDC.clear();
        }
    }

    @PutMapping("/{matchId}")
    @Operation(summary = "Update match details",
            description = "Updates an existing match's details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Match updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Match not found")
    })
    public org.fivy.matchservice.api.dto.ApiResponse<MatchResponse> updateMatch(
            @PathVariable UUID matchId,
            @Valid @RequestBody UpdateMatchRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("User {} updating match: {}", currentUserId, matchId);
        MatchResponse updatedMatch = matchService.updateMatch(matchId, request);
        return org.fivy.matchservice.api.dto.ApiResponse.success(updatedMatch);
    }

    @GetMapping("/{matchId}")
    @Operation(summary = "Get match details",
            description = "Retrieves details of a specific match")
    public org.fivy.matchservice.api.dto.ApiResponse<MatchResponse> getMatch(
            @PathVariable UUID matchId,
            Authentication authentication
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("User {} fetching match: {}", currentUserId, matchId);
        MatchResponse match = matchService.getMatch(matchId, currentUserId);
        return org.fivy.matchservice.api.dto.ApiResponse.success(match);
    }

    @GetMapping("/{matchId}/details")
    @Operation(summary = "Get match details",
            description = "Retrieves details of a specific match")
    public org.fivy.matchservice.api.dto.ApiResponse<MatchDetailsResponse> getMatchDetails(
            @PathVariable UUID matchId,
            Authentication authentication
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("User {} fetching match details: {}", currentUserId, matchId);
        MatchDetailsResponse match = matchService.getMatchWithDetails(matchId, currentUserId);
        return org.fivy.matchservice.api.dto.ApiResponse.success(match);
    }

    @GetMapping("/history/{matchId}")
    @Operation(summary = "Get specific match from user's history",
            description = "Retrieves detailed information about a specific match from the user's history")
    public org.fivy.matchservice.api.dto.ApiResponse<MatchHistoryResponse> getMatchHistoryDetail(
            @PathVariable UUID matchId,
            Authentication authentication
    ) {
        UUID userId = extractUserIdFromJwt(authentication);
        log.info("Fetching match history detail for matchId: {} requested by user: {}", matchId, userId);
        MatchHistoryResponse match = matchService.getMatchHistoryDetail(matchId, userId);
        return org.fivy.matchservice.api.dto.ApiResponse.success(match);
    }

    @GetMapping
    @Operation(summary = "Get matches with optional filters",
            description = "Retrieves a paginated list of matches, optionally filtered by location/distance.")
    public org.fivy.matchservice.api.dto.ApiResponse<Page<MatchResponse>> getMatches(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double distance,
            @RequestParam(required = false) String skillLevel,
            Pageable pageable,
            Authentication authentication
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("User {} fetching matches page: {} with lat={}, lon={}, dist={}, skill={}",
                currentUserId, pageable.getPageNumber(), latitude, longitude, distance, skillLevel);

        Page<MatchResponse> matches = matchService.getMatches(
                latitude,
                longitude,
                distance,
                skillLevel,
                pageable,
                currentUserId
        );

        return org.fivy.matchservice.api.dto.ApiResponse.success(matches);
    }

    @GetMapping("/user/{creatorId}/matches")
    @Operation(summary = "Get user's matches",
            description = "Retrieves matches created by the authenticated user")
    public org.fivy.matchservice.api.dto.ApiResponse<Page<MatchResponse>> getMyMatches(
            @PathVariable UUID creatorId,
            Authentication authentication,
            Pageable pageable
    ) {
        UUID userId = extractUserIdFromJwt(authentication);
        log.info("Fetching matches for user: {}", userId);
        Page<MatchResponse> matches = matchService.getMatchesByCreator(creatorId, pageable);
        return org.fivy.matchservice.api.dto.ApiResponse.success(matches);
    }

    @GetMapping("/search")
    @Operation(summary = "Search matches with various filters")
    public org.fivy.matchservice.api.dto.ApiResponse<Page<MatchResponse>> searchMatches(
            @Valid FilterMatchesRequest filterRequest,
            Pageable pageable,
            Authentication authentication
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("User {} searching matches with filters: {}", currentUserId, filterRequest);

        Page<MatchResponse> matches = matchService.searchMatches(filterRequest, pageable, currentUserId);
        return org.fivy.matchservice.api.dto.ApiResponse.success(matches);
    }

    @DeleteMapping("/{matchId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete match",
            description = "Deletes a match if it's in DRAFT or OPEN status")
    public org.fivy.matchservice.api.dto.ApiResponse<Void> deleteMatch(
            @PathVariable UUID matchId,
            Authentication authentication
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("User {} deleting match: {}", currentUserId, matchId);
        matchService.deleteMatch(matchId);
        return org.fivy.matchservice.api.dto.ApiResponse.success(null);
    }

    @PatchMapping("/{matchId}/status")
    @Operation(summary = "Update match status",
            description = "Updates the status of a match")
    public org.fivy.matchservice.api.dto.ApiResponse<MatchResponse> updateMatchStatus(
            @PathVariable UUID matchId,
            @RequestParam MatchStatus status,
            Authentication authentication
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("User {} updating status for match: {} to: {}", currentUserId, matchId, status);
        MatchResponse updatedMatch = matchService.updateMatchStatus(matchId, status);
        return org.fivy.matchservice.api.dto.ApiResponse.success(updatedMatch);
    }

    @GetMapping("/history")
    @Operation(summary = "Get user's match history",
            description = "Retrieves a paginated list of matches the user has participated in")
    public org.fivy.matchservice.api.dto.ApiResponse<Page<MatchHistoryResponse>> getMatchHistory(
            Authentication authentication,
            Pageable pageable
    ) {
        UUID userId = extractUserIdFromJwt(authentication);
        log.info("Fetching match history for user: {}", userId);
        Page<MatchHistoryResponse> matches = matchService.getMatchHistory(userId, pageable);
        return org.fivy.matchservice.api.dto.ApiResponse.success(matches);
    }

    @GetMapping("/user/{userId}/upcoming")
    @Operation(summary = "Get user's upcoming matches",
            description = "Retrieves all upcoming matches that a specific user is participating in")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved upcoming matches"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    public org.fivy.matchservice.api.dto.ApiResponse<Page<MatchResponse>> getUserUpcomingMatches(
            @PathVariable UUID userId,
            Pageable pageable,
            Authentication authentication
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("User {} fetching upcoming matches for user: {}", currentUserId, userId);
        Page<MatchResponse> matches = matchService.getUserUpcomingMatches(userId, pageable);
        return org.fivy.matchservice.api.dto.ApiResponse.success(matches);
    }
}