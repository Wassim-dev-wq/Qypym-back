package org.fivy.matchservice.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.api.dto.response.MatchJoinRequestResponse;
import org.fivy.matchservice.application.service.MatchJoinRequestService;
import org.fivy.matchservice.domain.entity.Match;
import org.fivy.matchservice.domain.entity.MatchJoinRequest;
import org.fivy.matchservice.domain.entity.MatchPlayer;
import org.fivy.matchservice.domain.entity.MatchTeam;
import org.fivy.matchservice.domain.enums.JoinRequestStatus;
import org.fivy.matchservice.domain.enums.MatchStatus;
import org.fivy.matchservice.domain.enums.PlayerRole;
import org.fivy.matchservice.domain.enums.PlayerStatus;
import org.fivy.matchservice.domain.event.MatchEventType;
import org.fivy.matchservice.domain.repository.MatchJoinRequestRepository;
import org.fivy.matchservice.domain.repository.MatchPlayerRepository;
import org.fivy.matchservice.domain.repository.MatchRepository;
import org.fivy.matchservice.domain.repository.MatchTeamRepository;
import org.fivy.matchservice.shared.exception.MatchException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MatchJoinRequestServiceImpl implements MatchJoinRequestService {

    private final MatchRepository matchRepository;
    private final MatchJoinRequestRepository matchJoinRequestRepository;
    private final MatchTeamRepository matchTeamRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final NotificationService notificationService;

    @Override
    public MatchJoinRequestResponse requestToJoin(
            UUID matchId,
            UUID userId,
            UUID preferredTeamId,
            String message,
            String position,
            String experience,
            String personalNote,
            boolean isAvailable
    ) {
        log.debug("User {} requesting to join match {} with team {}", userId, matchId, preferredTeamId);

        Match match = matchRepository.findById(matchId).orElseThrow(
                () -> new MatchException("Match not found: " + matchId, "MATCH_NOT_FOUND", HttpStatus.NOT_FOUND)
        );
        if (match.getStatus() != MatchStatus.OPEN) {
            throw new MatchException("Match is not open for requests", "MATCH_NOT_OPEN", HttpStatus.BAD_REQUEST);
        }
        if(match.getCreatorId().equals(userId)) {
            throw new MatchException("Creator cannot request to join his own match", "CREATOR_CANNOT_JOIN", HttpStatus.BAD_REQUEST);
        }

        MatchTeam preferredTeam = null;
        if (preferredTeamId != null) {
            preferredTeam = matchTeamRepository.findById(preferredTeamId)
                    .orElseThrow(() -> new MatchException(
                            "Preferred team not found: " + preferredTeamId,
                            "TEAM_NOT_FOUND",
                            HttpStatus.NOT_FOUND
                    ));

            if (!preferredTeam.getMatch().getId().equals(matchId)) {
                throw new MatchException(
                        "Preferred team does not belong to this match",
                        "INVALID_TEAM_MATCH",
                        HttpStatus.BAD_REQUEST
                );
            }
        }

        Optional<MatchJoinRequest> existingRequestOpt = matchJoinRequestRepository.findByMatchIdAndUserId(matchId, userId);
        if (existingRequestOpt.isPresent()) {
            MatchJoinRequest existingReq = existingRequestOpt.get();
            if (existingReq.getRequestStatus() == JoinRequestStatus.CANCELED) {
                existingReq.setRequestStatus(JoinRequestStatus.PENDING);
                existingReq.setPreferredTeam(preferredTeam);
                existingReq.setMessage(message);
                existingReq.setPosition(position);
                existingReq.setExperience(experience);
                existingReq.setPersonalNote(personalNote);
                existingReq.setAvailable(isAvailable);

                MatchJoinRequest saved = matchJoinRequestRepository.save(existingReq);
                log.info("User {} re-requested to join match {}", userId, matchId);

                sendJoinRequestNotification(match, personNameFromPosition(position));

                return toResponse(saved);
            }
            if (existingReq.getRequestStatus() == JoinRequestStatus.PENDING ||
                    existingReq.getRequestStatus() == JoinRequestStatus.ACCEPTED) {
                throw new MatchException("User already joined/requested this match",
                        "DUPLICATE_REQUEST", HttpStatus.BAD_REQUEST);
            }
        }

        MatchJoinRequest joinRequest = MatchJoinRequest.builder()
                .match(match)
                .userId(userId)
                .preferredTeam(preferredTeam)
                .message(message)
                .position(position)
                .experience(experience)
                .personalNote(personalNote)
                .isAvailable(isAvailable)
                .requestStatus(JoinRequestStatus.PENDING)
                .build();

        MatchJoinRequest saved = matchJoinRequestRepository.save(joinRequest);
        log.info("User {} successfully requested to join match {}", userId, matchId);
        sendJoinRequestNotification(match, personNameFromPosition(position));
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MatchJoinRequestResponse getJoinRequest(UUID matchId, UUID userId) {
        log.debug("Fetching join request for user {} and match {}", userId, matchId);
        return findRequest(matchId, userId)
                .map(this::toResponse)
                .orElse(MatchJoinRequestResponse.builder()
                        .matchId(matchId)
                        .userId(userId)
                        .requestStatus(JoinRequestStatus.NOT_REQUESTED)
                        .build());
    }

    @Override
    public MatchJoinRequestResponse acceptJoinRequest(UUID requestId, UUID ownerId, UUID assignedTeamId) {
        log.debug("Owner {} is accepting join request {} with team {}", ownerId, requestId, assignedTeamId);
        MatchJoinRequest joinRequest = matchJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new MatchException(
                        "Join request not found: " + requestId,
                        "JOIN_REQUEST_NOT_FOUND",
                        HttpStatus.NOT_FOUND
                ));
        Match match = joinRequest.getMatch();
        UUID matchId = match.getId();
        if (!match.getCreatorId().equals(ownerId)) {
            throw new MatchException(
                    "User is not the match owner",
                    "NOT_MATCH_OWNER",
                    HttpStatus.FORBIDDEN
            );
        }
        if (joinRequest.getRequestStatus() != JoinRequestStatus.PENDING) {
            throw new MatchException(
                    "Join request is not in PENDING status",
                    "INVALID_REQUEST_STATUS",
                    HttpStatus.BAD_REQUEST
            );
        }
        MatchTeam assignedTeam = null;
        if (assignedTeamId != null) {
            assignedTeam = matchTeamRepository.findById(assignedTeamId)
                    .orElseThrow(() -> new MatchException(
                            "Assigned team not found: " + assignedTeamId,
                            "TEAM_NOT_FOUND",
                            HttpStatus.NOT_FOUND
                    ));
            if (!assignedTeam.getMatch().getId().equals(matchId)) {
                throw new MatchException(
                        "Assigned team does not belong to this match",
                        "INVALID_TEAM_MATCH",
                        HttpStatus.BAD_REQUEST
                );
            }
        }
        joinRequest.setRequestStatus(JoinRequestStatus.ACCEPTED);
        if (assignedTeam != null) {
            joinRequest.setPreferredTeam(assignedTeam);
        }
        MatchJoinRequest saved = matchJoinRequestRepository.save(joinRequest);
        log.info("Join request {} accepted for match {}", requestId, matchId);
        createMatchPlayerFromJoinRequest(match, saved);
        notificationService.sendJoinRequestResponseNotification(match, joinRequest.getUserId(), true);
        Map<String, String> eventData = new HashMap<>();
        eventData.put("playerId", joinRequest.getUserId().toString());
        eventData.put("playerName", personNameFromPosition(joinRequest.getPosition()));
        match.setLastEventData(eventData);
        notificationService.sendMatchEventNotifications(match, MatchEventType.PLAYER_JOINED);
        return toResponse(saved);
    }

    @Override
    public MatchJoinRequestResponse rejectJoinRequest(UUID matchId, UUID requestId, UUID ownerId) {
        log.debug("Owner {} is rejecting join request {} for match {}", ownerId, requestId, matchId);
        Match match = validateMatchOwner(matchId, ownerId);
        MatchJoinRequest joinRequest = validateJoinRequest(matchId, requestId);
        joinRequest.setRequestStatus(JoinRequestStatus.DECLINED);
        MatchJoinRequest saved = matchJoinRequestRepository.save(joinRequest);
        log.info("Join request {} rejected for match {}", requestId, matchId);
        notificationService.sendJoinRequestResponseNotification(match, joinRequest.getUserId(), false);
        return toResponse(saved);
    }

    @Override
    public MatchJoinRequestResponse cancelJoinRequest(UUID requestId, UUID userId) {
        log.debug("User {} is cancelling join request {}", userId, requestId);

        MatchJoinRequest joinRequest = matchJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new MatchException(
                        "Join request not found: " + requestId,
                        "JOIN_REQUEST_NOT_FOUND",
                        HttpStatus.NOT_FOUND
                ));

        if (!joinRequest.getUserId().equals(userId)) {
            throw new MatchException(
                    "User is not the join request owner",
                    "NOT_REQUEST_OWNER",
                    HttpStatus.FORBIDDEN
            );
        }

        if (joinRequest.getRequestStatus() != JoinRequestStatus.PENDING) {
            throw new MatchException(
                    "Join request is not in PENDING status",
                    "INVALID_REQUEST_STATUS",
                    HttpStatus.BAD_REQUEST
            );
        }

        joinRequest.setRequestStatus(JoinRequestStatus.CANCELED);
        MatchJoinRequest saved = matchJoinRequestRepository.save(joinRequest);
        log.info("User {} successfully cancelled join request {}", userId, requestId);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchJoinRequestResponse> getJoinRequests(UUID matchId, UUID userId) {
        log.debug("Fetching all join requests for match {} by user {}", matchId, userId);
        validateMatchOwner(matchId, userId);
        List<MatchJoinRequest> requests = matchJoinRequestRepository.findAllByMatchId(matchId);
        return requests.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<MatchJoinRequestResponse> getUserJoinRequests(UUID userId) {
        log.debug("Fetching all join requests for user {}", userId);
        List<MatchJoinRequest> requests = matchJoinRequestRepository.findAllByUserId(userId);
        return requests.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private Match validateMatchOwner(UUID matchId, UUID ownerId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchException(
                        "Match not found: " + matchId,
                        "MATCH_NOT_FOUND",
                        HttpStatus.NOT_FOUND
                ));

        if (!match.getCreatorId().equals(ownerId)) {
            throw new MatchException(
                    "User is not the match owner",
                    "NOT_MATCH_OWNER",
                    HttpStatus.FORBIDDEN
            );
        }

        return match;
    }

    private MatchJoinRequest validateJoinRequest(UUID matchId, UUID requestId) {
        MatchJoinRequest joinRequest = matchJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new MatchException(
                        "Join request not found: " + requestId,
                        "JOIN_REQUEST_NOT_FOUND",
                        HttpStatus.NOT_FOUND
                ));

        if (!joinRequest.getMatch().getId().equals(matchId)) {
            throw new MatchException(
                    "Join request does not belong to this match",
                    "INVALID_REQUEST_MATCH",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (joinRequest.getRequestStatus() != JoinRequestStatus.PENDING) {
            throw new MatchException(
                    "Join request is not in PENDING status",
                    "INVALID_REQUEST_STATUS",
                    HttpStatus.BAD_REQUEST
            );
        }

        return joinRequest;
    }

    @Transactional(readOnly = true)
    protected Optional<MatchJoinRequest> findRequest(UUID matchId, UUID userId) {
        return matchJoinRequestRepository.findByMatchIdAndUserId(matchId, userId);
    }

    private void createMatchPlayerFromJoinRequest(Match match, MatchJoinRequest joinRequest) {
        PlayerRole role = convertPositionToPlayerRole(joinRequest.getPosition());

        MatchPlayer newPlayer = MatchPlayer.builder()
                .match(match)
                .team(joinRequest.getPreferredTeam())
                .playerId(joinRequest.getUserId())
                .role(role)
                .status(PlayerStatus.JOINED)
                .joinedAt(LocalDateTime.now())
                .build();

        matchPlayerRepository.save(newPlayer);
        log.info("Created MatchPlayer for user {} in match {}", joinRequest.getUserId(), match.getId());
    }

    private PlayerRole convertPositionToPlayerRole(String position) {
        if (position == null) {
            return PlayerRole.UNKNOWN;
        }
        return switch (position.toLowerCase()) {
            case "gardien" -> PlayerRole.GOALKEEPER;
            case "défenseur" -> PlayerRole.DEFENDER;
            case "milieu" -> PlayerRole.MIDFIELDER;
            case "attaquant" -> PlayerRole.FORWARD;
            default -> PlayerRole.UNKNOWN;
        };
    }

    private String personNameFromPosition(String position) {
        if (position == null) {
            return "Un joueur";
        }
        return switch (position.toLowerCase()) {
            case "gardien" -> "Un gardien";
            case "défenseur" -> "Un défenseur";
            case "milieu" -> "Un milieu";
            case "attaquant" -> "Un attaquant";
            default -> "Un joueur";
        };
    }

    private void sendJoinRequestNotification(Match match, String requesterName) {
        notificationService.sendJoinRequestNotification(match, requesterName);
    }

    private MatchJoinRequestResponse toResponse(MatchJoinRequest entity) {
        return MatchJoinRequestResponse.builder()
                .id(entity.getId())
                .matchId(entity.getMatch().getId())
                .userId(entity.getUserId())
                .preferredTeamId(entity.getPreferredTeam() != null ? entity.getPreferredTeam().getId() : null)
                .message(entity.getMessage())
                .position(entity.getPosition())
                .experience(entity.getExperience())
                .personalNote(entity.getPersonalNote())
                .isAvailable(entity.isAvailable())
                .requestStatus(entity.getRequestStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}