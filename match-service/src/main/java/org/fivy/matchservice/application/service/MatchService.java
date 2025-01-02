package org.fivy.matchservice.application.service;

import org.fivy.matchservice.api.dto.request.CreateMatchRequest;
import org.fivy.matchservice.api.dto.request.UpdateMatchRequest;
import org.fivy.matchservice.api.dto.response.MatchResponse;
import org.fivy.matchservice.domain.enums.MatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MatchService {
    MatchResponse createMatch(UUID creatorId, CreateMatchRequest request);
    MatchResponse updateMatch(UUID matchId, UpdateMatchRequest request);
    MatchResponse getMatch(UUID matchId);
    Page<MatchResponse> getMatches(Pageable pageable);
    Page<MatchResponse> getMatchesByCreator(UUID creatorId, Pageable pageable);
    void deleteMatch(UUID matchId);
    MatchResponse updateMatchStatus(UUID matchId, MatchStatus newStatus);
}
