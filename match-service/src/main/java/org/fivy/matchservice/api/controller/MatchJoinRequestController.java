package org.fivy.matchservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.api.dto.response.MatchJoinRequestResponse;
import org.fivy.matchservice.application.service.MatchJoinRequestService;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    @Operation(summary = "Request to join a match",
            description = "Request to join a match")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Request to join a match created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public org.fivy.matchservice.api.dto.ApiResponse<MatchJoinRequestResponse> requestToJoin(
            @PathVariable UUID matchId,
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        MDC.put("correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString());
        log.info("Processing match join request");

        try {
            MatchJoinRequestResponse matchJoinRequestResponse = matchJoinRequestService.requestToJoin(matchId, UUID.fromString(userId));
            return org.fivy.matchservice.api.dto.ApiResponse.success(matchJoinRequestResponse);
        } finally {
            MDC.clear();
        }
    }
}
