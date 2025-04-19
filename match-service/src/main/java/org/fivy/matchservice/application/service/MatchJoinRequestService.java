package org.fivy.matchservice.application.service;

import org.fivy.matchservice.api.dto.response.MatchJoinRequestResponse;

import java.util.List;
import java.util.UUID;

public interface MatchJoinRequestService {

    MatchJoinRequestResponse requestToJoin(
            UUID matchId,
            UUID userId,
            UUID preferredTeamId,
            String message,
            String position,
            String experience,
            String personalNote,
            boolean isAvailable
    );

    MatchJoinRequestResponse getJoinRequest(UUID matchId, UUID userId);
    MatchJoinRequestResponse acceptJoinRequest(UUID requestId, UUID ownerId, UUID assignedTeamId);

    MatchJoinRequestResponse rejectJoinRequest(UUID matchId, UUID requestId, UUID ownerId);
    List<MatchJoinRequestResponse> getJoinRequests(UUID matchId, UUID userId);

    List<MatchJoinRequestResponse> getUserJoinRequests(UUID userId);

    MatchJoinRequestResponse cancelJoinRequest(UUID requestId, UUID uuid);

}
