package org.fivy.matchservice.application.service;

import org.fivy.matchservice.api.dto.response.MatchPlayerResponse;
import org.fivy.matchservice.domain.enums.PlayerStatus;

import java.util.List;
import java.util.UUID;

public interface MatchPlayerService {
    void updatePlayerStatus(UUID playerId, UUID currentUserId, PlayerStatus status);

    MatchPlayerResponse getPlayerStatus(UUID playerId, UUID currentUserId);

    List<MatchPlayerResponse> getPlayersStatus(UUID currentUserId);

    void leaveMatch(UUID requestId, UUID matchId, UUID userId);
}
