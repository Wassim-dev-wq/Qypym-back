package org.fivy.matchservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.api.dto.request.CreateMatchRequest;
import org.fivy.matchservice.api.dto.request.UpdateMatchRequest;
import org.fivy.matchservice.api.dto.response.MatchResponse;
import org.fivy.matchservice.application.service.MatchService;
import org.fivy.matchservice.domain.enums.MatchStatus;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
@Slf4j
@Validated
//@SecurityRequirement(name = "bearer-auth")
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
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody CreateMatchRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        MDC.put("correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString());
        log.info("Processing match creation request");

        try {
            UUID creatorId = UUID.fromString(userId);
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
            @Valid @RequestBody UpdateMatchRequest request
    ) {
        log.info("Updating match: {}", matchId);
        MatchResponse updatedMatch = matchService.updateMatch(matchId, request);
        return org.fivy.matchservice.api.dto.ApiResponse.success(updatedMatch);
    }

    @GetMapping("/{matchId}")
    @Operation(summary = "Get match details",
            description = "Retrieves details of a specific match")
    public org.fivy.matchservice.api.dto.ApiResponse<MatchResponse> getMatch(@PathVariable UUID matchId) {
        log.info("Fetching match: {}", matchId);
        MatchResponse match = matchService.getMatch(matchId);
        return org.fivy.matchservice.api.dto.ApiResponse.success(match);
    }

    @GetMapping
    @Operation(summary = "Get matches",
            description = "Retrieves a paginated list of matches")
    public org.fivy.matchservice.api.dto.ApiResponse<Page<MatchResponse>> getMatches(Pageable pageable) {
        log.info("Fetching matches page: {}", pageable.getPageNumber());
        Page<MatchResponse> matches = matchService.getMatches(pageable);
        return org.fivy.matchservice.api.dto.ApiResponse.success(matches);
    }

    @GetMapping("/my-matches")
    @Operation(summary = "Get user's matches",
            description = "Retrieves matches created by the authenticated user")
    public org.fivy.matchservice.api.dto.ApiResponse<Page<MatchResponse>> getMyMatches(
            @AuthenticationPrincipal Jwt jwt,
            Pageable pageable
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("Fetching matches for user: {}", userId);
        Page<MatchResponse> matches = matchService.getMatchesByCreator(userId, pageable);
        return org.fivy.matchservice.api.dto.ApiResponse.success(matches);
    }

    @DeleteMapping("/{matchId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete match",
            description = "Deletes a match if it's in DRAFT or OPEN status")
    public org.fivy.matchservice.api.dto.ApiResponse<Void> deleteMatch(@PathVariable UUID matchId) {
        log.info("Deleting match: {}", matchId);
        matchService.deleteMatch(matchId);
        return org.fivy.matchservice.api.dto.ApiResponse.success(null);
    }

    @PatchMapping("/{matchId}/status")
    @Operation(summary = "Update match status",
            description = "Updates the status of a match")
    public org.fivy.matchservice.api.dto.ApiResponse<MatchResponse> updateMatchStatus(
            @PathVariable UUID matchId,
            @RequestParam MatchStatus status
    ) {
        log.info("Updating status for match: {} to: {}", matchId, status);
        MatchResponse updatedMatch = matchService.updateMatchStatus(matchId, status);
        return org.fivy.matchservice.api.dto.ApiResponse.success(updatedMatch);
    }
}
