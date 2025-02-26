package org.fivy.matchservice.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.api.dto.response.MatchPlayerResponse;
import org.fivy.matchservice.application.service.MatchPlayerService;
import org.fivy.matchservice.domain.entity.MatchPlayer;
import org.fivy.matchservice.domain.enums.PlayerStatus;
import org.fivy.matchservice.domain.repository.MatchPlayerRepository;
import org.fivy.matchservice.shared.exception.MatchException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MatchPlayerServiceImpl implements MatchPlayerService {

    private final MatchPlayerRepository matchPlayerRepository;

    @Override
    public void updatePlayerStatus(UUID playerId, UUID currentUserId, PlayerStatus status) {
        log.debug("Updating player status for playerId {} by user {} to {}", playerId, currentUserId, status);

        MatchPlayer player = matchPlayerRepository.findById(playerId)
                .orElseThrow(() -> new MatchException("Player not found with id: " + playerId,
                        "PLAYER_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!player.getPlayerId().equals(currentUserId) &&
                !player.getMatch().getCreatorId().equals(currentUserId)) {
            throw new MatchException("Not authorized to update player status",
                    "FORBIDDEN", HttpStatus.FORBIDDEN);
        }

        player.setStatus(status);
        matchPlayerRepository.save(player);
        log.debug("Player status updated for playerId {} to {}", playerId, status);
    }

    @Override
    public MatchPlayerResponse getPlayerStatus(UUID playerId, UUID currentUserId) {
        log.debug("Fetching player status for playerId {} by user {}", playerId, currentUserId);

        MatchPlayer player = matchPlayerRepository.findById(playerId)
                .orElseThrow(() -> new MatchException("Player not found with id: " + playerId,
                        "PLAYER_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!player.getPlayerId().equals(currentUserId) &&
                !player.getMatch().getCreatorId().equals(currentUserId)) {
            throw new MatchException("Not authorized to view player status",
                    "FORBIDDEN", HttpStatus.FORBIDDEN);
        }

        MatchPlayerResponse response = new MatchPlayerResponse();
        response.setPlayerId(player.getPlayerId());
        response.setStatus(player.getStatus());
        log.debug("Returning player status for playerId {}: {}", playerId, response);
        return response;
    }

    @Override
    public List<MatchPlayerResponse> getPlayersStatus(UUID currentUserId) {
        log.debug("Fetching all player statuses for user {}", currentUserId);

        List<MatchPlayer> players = matchPlayerRepository.findAllByPlayerId(currentUserId);
        List<MatchPlayerResponse> responses = players.stream().map(player -> {
            MatchPlayerResponse response = new MatchPlayerResponse();
            response.setPlayerId(player.getPlayerId());
            response.setStatus(player.getStatus());
            return response;
        }).collect(Collectors.toList());

        log.debug("Found {} player status records for user {}", responses.size(), currentUserId);
        return responses;
    }
}
