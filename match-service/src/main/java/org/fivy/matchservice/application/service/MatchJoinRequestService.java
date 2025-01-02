package org.fivy.matchservice.application.service;

import org.fivy.matchservice.api.dto.response.MatchJoinRequestResponse;

import java.util.UUID;

public interface MatchJoinRequestService {

    MatchJoinRequestResponse requestToJoin(UUID matchId, UUID userId);
}
