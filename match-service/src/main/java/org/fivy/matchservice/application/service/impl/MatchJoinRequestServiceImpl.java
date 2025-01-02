package org.fivy.matchservice.application.service.impl;

import org.fivy.matchservice.api.dto.response.MatchJoinRequestResponse;
import org.fivy.matchservice.application.service.MatchJoinRequestService;
import org.fivy.matchservice.domain.entity.Match;
import org.fivy.matchservice.domain.entity.MatchJoinRequest;
import org.fivy.matchservice.domain.enums.JoinRequestStatus;
import org.fivy.matchservice.domain.enums.MatchStatus;
import org.fivy.matchservice.domain.repository.MatchJoinRequestRepository;
import org.fivy.matchservice.domain.repository.MatchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MatchJoinRequestServiceImpl implements MatchJoinRequestService {

    private final MatchRepository matchRepository;
    private final MatchJoinRequestRepository matchJoinRequestRepository;

    public MatchJoinRequestServiceImpl(MatchRepository matchRepository, MatchJoinRequestRepository matchJoinRequestRepository) {
        this.matchRepository = matchRepository;
        this.matchJoinRequestRepository = matchJoinRequestRepository;
    }

    @Override
    public MatchJoinRequestResponse requestToJoin(UUID matchId, UUID userId) {
        Match match = matchRepository.findById(matchId).orElseThrow(() -> new org.fivy.matchservice.shared.exception.MatchException("Match not found: " + matchId, "MATCH_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (match.getStatus() == MatchStatus.CANCELLED || match.getStatus() == MatchStatus.COMPLETED || match.getStatus() == MatchStatus.IN_PROGRESS) {
            throw new IllegalStateException("Match is not open for requests");
        }
        boolean alreadyRequested = matchJoinRequestRepository.existsByMatchIdAndUserIdAndRequestStatusIn(matchId, userId, List.of(JoinRequestStatus.PENDING, JoinRequestStatus.ACCEPTED));
        if (alreadyRequested) {
            throw new IllegalStateException("User already joined or requested to join this match");
        }
        MatchJoinRequest joinRequest = MatchJoinRequest.builder().match(match).userId(userId).requestStatus(JoinRequestStatus.PENDING).build();

        MatchJoinRequest saved = matchJoinRequestRepository.save(joinRequest);

        return toResponse(saved);
    }

    private MatchJoinRequestResponse toResponse(MatchJoinRequest entity) {
        return MatchJoinRequestResponse.builder().id(entity.getId()).matchId(entity.getMatch().getId()).userId(entity.getUserId()).requestStatus(entity.getRequestStatus()).createdAt(entity.getCreatedAt()).updatedAt(entity.getUpdatedAt()).build();
    }

}
