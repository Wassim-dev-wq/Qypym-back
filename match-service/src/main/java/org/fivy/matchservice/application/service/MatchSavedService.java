package org.fivy.matchservice.application.service;

import org.fivy.matchservice.api.dto.response.MatchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MatchSavedService {
    void saveMatch(UUID matchId, UUID userId);
    void unsaveMatch(UUID matchId, UUID userId);
    Page<MatchResponse> getSavedMatches(UUID userId, Pageable pageable);
    boolean isMatchSaved(UUID matchId, UUID userId);
}
