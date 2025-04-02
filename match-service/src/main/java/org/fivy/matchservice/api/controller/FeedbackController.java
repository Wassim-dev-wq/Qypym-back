package org.fivy.matchservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.api.dto.request.SubmitMatchFeedbackRequest;
import org.fivy.matchservice.api.dto.response.FeedbackRequestResponse;
import org.fivy.matchservice.api.dto.response.PlayerRatingSummaryResponse;
import org.fivy.matchservice.application.service.FeedbackService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.fivy.matchservice.shared.JwtConverter.extractUserIdFromJwt;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
@Slf4j
@Validated
@SecurityRequirement(name = "bearer-auth")
@PreAuthorize("isAuthenticated()")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping("/matches/{matchId}")
    @Operation(summary = "Get feedback request for a match",
            description = "Retrieves the feedback request for a specific match")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feedback request retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Feedback request not found")
    })
    public org.fivy.matchservice.api.dto.ApiResponse<FeedbackRequestResponse> getFeedbackRequest(
            @PathVariable UUID matchId,
            Authentication authentication
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("User {} fetching feedback request for match: {}", currentUserId, matchId);
        FeedbackRequestResponse response = feedbackService.getFeedbackRequest(matchId, currentUserId);
        return org.fivy.matchservice.api.dto.ApiResponse.success(response);
    }

    @PostMapping("/requests/{requestId}/submit")
    @Operation(summary = "Submit feedback for a match",
            description = "Submits player's feedback for a match including ratings for other players")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feedback submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Feedback request not found")
    })
    public org.fivy.matchservice.api.dto.ApiResponse<FeedbackRequestResponse> submitFeedback(
            @PathVariable UUID requestId,
            @Valid @RequestBody SubmitMatchFeedbackRequest request,
            Authentication authentication
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("User {} submitting feedback for request: {}", currentUserId, requestId);
        FeedbackRequestResponse response = feedbackService.submitFeedback(requestId, currentUserId, request);
        return org.fivy.matchservice.api.dto.ApiResponse.success(response);
    }

    @GetMapping("/players/{playerId}/rating")
    @Operation(summary = "Get player rating summary",
            description = "Retrieves the rating summary for a specific player")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rating summary retrieved successfully")
    })
    public org.fivy.matchservice.api.dto.ApiResponse<PlayerRatingSummaryResponse> getPlayerRatingSummary(
            @PathVariable UUID playerId,
            Authentication authentication
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("User {} fetching rating summary for player: {}", currentUserId, playerId);
        PlayerRatingSummaryResponse response = feedbackService.getPlayerRatingSummary(playerId);
        return org.fivy.matchservice.api.dto.ApiResponse.success(response);
    }

    @GetMapping("/players/top-rated")
    @Operation(summary = "Get top rated players",
            description = "Retrieves a list of top rated players")
    public org.fivy.matchservice.api.dto.ApiResponse<List<PlayerRatingSummaryResponse>> getTopRatedPlayers(
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication
    ) {
        UUID currentUserId = extractUserIdFromJwt(authentication);
        log.info("User {} fetching top {} rated players", currentUserId, limit);
        List<PlayerRatingSummaryResponse> response = feedbackService.getTopRatedPlayers(limit);
        return org.fivy.matchservice.api.dto.ApiResponse.success(response);
    }
}