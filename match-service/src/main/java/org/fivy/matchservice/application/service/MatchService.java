package org.fivy.matchservice.application.service;

import org.fivy.matchservice.api.dto.request.CreateMatchRequest;
import org.fivy.matchservice.api.dto.request.UpdateMatchRequest;
import org.fivy.matchservice.api.dto.response.MatchDetailsResponse;
import org.fivy.matchservice.api.dto.response.MatchResponse;
import org.fivy.matchservice.domain.enums.MatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface MatchService {
    MatchResponse createMatch(UUID creatorId, CreateMatchRequest request);
    MatchResponse updateMatch(UUID matchId, UpdateMatchRequest request);

    @Transactional(readOnly = true)
    MatchResponse getMatch(UUID matchId, UUID currentUserId);

    @Transactional(readOnly = true)
    MatchDetailsResponse getMatchWithDetails(UUID matchId, UUID currentUserId);

    Page<MatchResponse> getMatches(Pageable pageable);
    Page<MatchResponse> getMatchesByCreator(UUID creatorId, Pageable pageable);
    void deleteMatch(UUID matchId);
    MatchResponse updateMatchStatus(UUID matchId, MatchStatus newStatus);
    Page<MatchResponse> getMatches(Double latitude, Double longitude, Double distance, String skillLevel, Pageable pageable, UUID currentUserId);

}
