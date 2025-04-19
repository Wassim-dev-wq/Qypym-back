package org.fivy.matchservice.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.api.dto.response.MatchPlayerResponse;
import org.fivy.matchservice.application.service.MatchPlayerService;
import org.fivy.matchservice.domain.entity.Match;
import org.fivy.matchservice.domain.entity.MatchJoinRequest;
import org.fivy.matchservice.domain.entity.MatchPlayer;
import org.fivy.matchservice.domain.enums.JoinRequestStatus;
import org.fivy.matchservice.domain.enums.MatchStatus;
import org.fivy.matchservice.domain.enums.PlayerStatus;
import org.fivy.matchservice.domain.event.MatchEvent;
import org.fivy.matchservice.domain.event.MatchEventType;
import org.fivy.matchservice.domain.repository.MatchJoinRequestRepository;
import org.fivy.matchservice.domain.repository.MatchPlayerRepository;
import org.fivy.matchservice.domain.repository.MatchRepository;
import org.fivy.matchservice.infrastructure.config.kafka.KafkaConfig;
import org.fivy.matchservice.shared.exception.MatchException;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MatchPlayerServiceImpl implements MatchPlayerService {

    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchJoinRequestRepository matchJoinRequestRepository;
    private final KafkaTemplate<String, MatchEvent> kafkaTemplate;
    private final NotificationService notificationService;

    @Override
    public void updatePlayerStatus(UUID playerId, UUID currentUserId, PlayerStatus status) {
        log.debug("Updating status to {} for player: {}", status, playerId);

        MatchPlayer player = matchPlayerRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new MatchException(
                        "Player not found with ID: " + playerId,
                        "PLAYER_NOT_FOUND",
                        HttpStatus.NOT_FOUND
                ));

        Match match = getMatch(currentUserId, player);

        player.setStatus(status);
        matchPlayerRepository.save(player);

        publishPlayerEvent(match, player, MatchEventType.PLAYER_STATUS_UPDATED);

        log.info("Updated status to {} for player: {} in match: {}", status, playerId, match.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public MatchPlayerResponse getPlayerStatus(UUID playerId, UUID currentUserId) {
        log.debug("Getting status for player: {}", playerId);

        MatchPlayer player = matchPlayerRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new MatchException(
                        "Player not found with ID: " + playerId,
                        "PLAYER_NOT_FOUND",
                        HttpStatus.NOT_FOUND
                ));

        Match match = player.getMatch();

        boolean isAuthorized = player.getPlayerId().equals(currentUserId) ||
                match.getCreatorId().equals(currentUserId) ||
                matchPlayerRepository.existsByMatchIdAndPlayerId(match.getId(), currentUserId);

        if (!isAuthorized) {
            throw new MatchException(
                    "Not authorized to view this player's status",
                    "NOT_AUTHORIZED",
                    HttpStatus.FORBIDDEN
            );
        }

        return mapToMatchPlayerResponse(player);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchPlayerResponse> getPlayersStatus(UUID currentUserId) {
        return null;
    }

    @Override
    @Transactional
    public void leaveMatch(UUID requestId, UUID matchId, UUID userId) {
        log.debug("Processing leave match request for user: {} in match: {}", userId, matchId);

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchException(
                        "Match not found with ID: " + matchId,
                        "MATCH_NOT_FOUND",
                        HttpStatus.NOT_FOUND
                ));

        if (match.getStatus() != MatchStatus.OPEN && match.getStatus() != MatchStatus.IN_PROGRESS) {
            throw new MatchException(
                    "Cannot leave match when it is in " + match.getStatus() + " state",
                    "INVALID_MATCH_STATE",
                    HttpStatus.BAD_REQUEST
            );
        }

        MatchPlayer player = matchPlayerRepository.findByMatchIdAndPlayerId(matchId, userId)
                .orElseThrow(() -> new MatchException(
                        "Player not found in this match",
                        "PLAYER_NOT_IN_MATCH",
                        HttpStatus.NOT_FOUND
                ));

        if (match.getCreatorId().equals(userId)) {
            log.warn("Match creator is leaving match: {}", matchId);
        }

        if (player.getStatus() == PlayerStatus.LEFT) {
            log.warn("Player {} has already left the match: {}", userId, matchId);
            return;
        }

        MatchJoinRequest matchJoinRequest = matchJoinRequestRepository.findByMatchIdAndUserId(matchId, userId)
                .orElseThrow(() -> new MatchException(
                        "Join request not found for user: " + userId,
                        "JOIN_REQUEST_NOT_FOUND",
                        HttpStatus.NOT_FOUND
                ));

        matchJoinRequest.setRequestStatus(JoinRequestStatus.LEFT);
        player.setStatus(PlayerStatus.LEFT);
        matchJoinRequestRepository.save(matchJoinRequest);
        matchPlayerRepository.save(player);
        log.info("User {} has left match: {}", userId, matchId);
    }


    private MatchPlayerResponse mapToMatchPlayerResponse(MatchPlayer player) {
        MatchPlayerResponse response = new MatchPlayerResponse();
        response.setPlayerId(player.getPlayerId());
        response.setMatchId(player.getMatch().getId());
        response.setStatus(player.getStatus());
        response.setRole(player.getRole());
        response.setJoinedAt(player.getJoinedAt());

        if (player.getTeam() != null) {
            response.setTeamId(player.getTeam().getId());
            response.setTeamName(player.getTeam().getName());
            response.setTeamNumber(player.getTeam().getTeamNumber());
        }

        return response;
    }

    private void publishPlayerEvent(Match match, MatchPlayer player, MatchEventType eventType) {
        log.debug("Publishing event: {} for player: {} in match: {}", eventType, player.getPlayerId(), match.getId());

        Map<String, String> eventData = new HashMap<>();
        eventData.put("playerId", player.getPlayerId().toString());
        eventData.put("status", player.getStatus().toString());

        if (player.getTeam() != null) {
            eventData.put("teamId", player.getTeam().getId().toString());
            eventData.put("teamName", player.getTeam().getName());
        }

        MatchEvent event = MatchEvent.builder()
                .type(eventType)
                .matchId(match.getId())
                .creatorId(match.getCreatorId())
                .data(eventData)
                .timestamp(ZonedDateTime.now().toLocalDateTime())
                .build();

        match.setLastEventData(eventData);
        kafkaTemplate.send(KafkaConfig.TOPIC_MATCH_EVENTS, match.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event: {}", eventType, ex);
                    } else {
                        log.debug("Event successfully published: {}", eventType);
                        notificationService.sendMatchEventNotifications(match, eventType);
                    }
                });
    }

    private static Match getMatch(UUID currentUserId, MatchPlayer player) {
        Match match = player.getMatch();
        if (match.getStatus() != MatchStatus.OPEN && match.getStatus() != MatchStatus.IN_PROGRESS) {
            throw new MatchException(
                    "Cannot update player status when match is in " + match.getStatus() + " state",
                    "INVALID_MATCH_STATE",
                    HttpStatus.BAD_REQUEST
            );
        }
        if (!player.getPlayerId().equals(currentUserId) && !match.getCreatorId().equals(currentUserId)) {
            throw new MatchException(
                    "Not authorized to update this player's status",
                    "NOT_AUTHORIZED",
                    HttpStatus.FORBIDDEN
            );
        }
        return match;
    }
}