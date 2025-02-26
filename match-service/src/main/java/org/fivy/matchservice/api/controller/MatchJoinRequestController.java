package org.fivy.matchservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.api.dto.request.MatchJoinRequestDto;
import org.fivy.matchservice.api.dto.response.MatchJoinRequestResponse;
import org.fivy.matchservice.application.service.MatchJoinRequestService;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matches/{matchId}/join")
@RequiredArgsConstructor
@Slf4j
@Validated
public class MatchJoinRequestController {

    private final MatchJoinRequestService matchJoinRequestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Request to join a match")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Request to join a match created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public org.fivy.matchservice.api.dto.ApiResponse<MatchJoinRequestResponse> requestToJoin(
            @PathVariable UUID matchId,
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            @Valid @RequestBody MatchJoinRequestDto request
    ) {
        MDC.put("correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString());
        log.info("Processing match join request for user: {}", userId);

        try {
            if (!request.isAvailable()) {
                throw new org.fivy.matchservice.shared.exception.MatchException("User must confirm availability", "AVAILABILITY_REQUIRED", HttpStatus.BAD_REQUEST);
            }

            MatchJoinRequestResponse response = matchJoinRequestService.requestToJoin(
                    matchId,
                    UUID.fromString(userId),
                    request.getPreferredTeamId(),
                    buildRequestMessage(request),
                    request.getPosition(),
                    request.getExperience(),
                    request.getPersonalNote(),
                    request.isAvailable()
            );
            return org.fivy.matchservice.api.dto.ApiResponse.success(response);
        } finally {
            MDC.clear();
        }
    }

    @DeleteMapping("/{requestId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Cancel a join request")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Join request canceled successfully"),
            @ApiResponse(responseCode = "404", description = "Join request not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public org.fivy.matchservice.api.dto.ApiResponse<MatchJoinRequestResponse> cancelJoinRequest(
            @PathVariable UUID matchId,
            @PathVariable UUID requestId,
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        MDC.put("correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString());
        try {
            MatchJoinRequestResponse response = matchJoinRequestService.cancelJoinRequest(matchId, requestId, UUID.fromString(userId));
            return org.fivy.matchservice.api.dto.ApiResponse.success(response);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get join request status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved join request status"),
            @ApiResponse(responseCode = "404", description = "Join request not found")
    })
    public org.fivy.matchservice.api.dto.ApiResponse<MatchJoinRequestResponse> getJoinRequestStatus(
            @PathVariable UUID matchId,
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        MDC.put("correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString());
        try {
            MatchJoinRequestResponse response = matchJoinRequestService.getJoinRequest(matchId, UUID.fromString(userId));
            return org.fivy.matchservice.api.dto.ApiResponse.success(response);
        } finally {
            MDC.clear();
        }
    }

    @PatchMapping("/{requestId}/accept")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Accept a join request")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Join request accepted successfully"),
            @ApiResponse(responseCode = "403", description = "User is not the match owner"),
            @ApiResponse(responseCode = "404", description = "Join request not found")
    })
    public org.fivy.matchservice.api.dto.ApiResponse<MatchJoinRequestResponse> acceptJoinRequest(
            @PathVariable UUID matchId,
            @PathVariable UUID requestId,
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            @RequestParam(required = false) UUID assignedTeamId
    ) {
        MDC.put("correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString());
        try {
            MatchJoinRequestResponse response = matchJoinRequestService.acceptJoinRequest(
                    matchId,
                    requestId,
                    UUID.fromString(userId),
                    assignedTeamId
            );
            return org.fivy.matchservice.api.dto.ApiResponse.success(response);
        } finally {
            MDC.clear();
        }
    }

    @PatchMapping("/{requestId}/reject")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Reject a join request")
    public org.fivy.matchservice.api.dto.ApiResponse<MatchJoinRequestResponse> rejectJoinRequest(
            @PathVariable UUID matchId,
            @PathVariable UUID requestId,
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        MDC.put("correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString());
        try {
            MatchJoinRequestResponse response = matchJoinRequestService.rejectJoinRequest(matchId, requestId, UUID.fromString(userId));
            return org.fivy.matchservice.api.dto.ApiResponse.success(response);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/requests")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get all join requests for a match")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved join requests"),
            @ApiResponse(responseCode = "403", description = "User is not the match owner"),
            @ApiResponse(responseCode = "404", description = "Match not found")
    })
    public org.fivy.matchservice.api.dto.ApiResponse<List<MatchJoinRequestResponse>> getJoinRequests(
            @PathVariable UUID matchId,
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        MDC.put("correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString());
        try {
            List<MatchJoinRequestResponse> responses = matchJoinRequestService.getJoinRequests(matchId, UUID.fromString(userId));
            return org.fivy.matchservice.api.dto.ApiResponse.success(responses);
        } finally {
            MDC.clear();
        }
    }

    private String buildRequestMessage(MatchJoinRequestDto req) {
        StringBuilder sb = new StringBuilder();
        if (req.getPosition() != null && !req.getPosition().isBlank()) {
            sb.append("Position: ").append(req.getPosition()).append("\n");
        }
        if (req.getExperience() != null && !req.getExperience().isBlank()) {
            sb.append("Exp: ").append(req.getExperience()).append("\n");
        }
        if (req.getPersonalNote() != null && !req.getPersonalNote().isBlank()) {
            sb.append("Note: ").append(req.getPersonalNote()).append("\n");
        }
        if (req.getMessage() != null && !req.getMessage().isBlank()) {
            sb.append("Message: ").append(req.getMessage()).append("\n");
        }
        return sb.toString().trim();
    }
}