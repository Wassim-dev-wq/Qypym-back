package org.fivy.matchservice.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.api.dto.request.CreateMatchRequest;
import org.fivy.matchservice.api.dto.request.UpdateMatchRequest;
import org.fivy.matchservice.api.dto.response.MatchResponse;
import org.fivy.matchservice.api.mapper.MatchMapper;
import org.fivy.matchservice.application.service.MatchService;
import org.fivy.matchservice.domain.entity.Match;
import org.fivy.matchservice.domain.entity.MatchPlayer;
import org.fivy.matchservice.domain.enums.MatchStatus;
import org.fivy.matchservice.domain.enums.PlayerRole;
import org.fivy.matchservice.domain.enums.PlayerStatus;
import org.fivy.matchservice.domain.event.MatchEvent;
import org.fivy.matchservice.domain.event.MatchEventType;
import org.fivy.matchservice.domain.repository.MatchPlayerRepository;
import org.fivy.matchservice.domain.repository.MatchRepository;
import org.fivy.matchservice.shared.exception.InvalidMatchStateException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MatchServiceImpl implements MatchService {
    private final MatchRepository matchRepository;
    private final MatchPlayerRepository playerRepository;
    private final MatchMapper matchMapper;
    private final KafkaTemplate<String, MatchEvent> kafkaTemplate;

    @Override
    public MatchResponse createMatch(UUID creatorId, CreateMatchRequest request) {
        log.debug("Creating match for creator: {}", creatorId);
        Match match = matchMapper.toEntity(request);
        match.setCreatorId(creatorId);
        match.setStatus(MatchStatus.DRAFT);
        match.setCreatedAt(ZonedDateTime.now());
        Match savedMatch = matchRepository.save(match);
        log.debug("Match created with ID: {}", savedMatch.getId());
        MatchPlayer creator = MatchPlayer.builder().match(savedMatch).playerId(creatorId).role(PlayerRole.CREATOR).status(PlayerStatus.JOINED).joinedAt(ZonedDateTime.now().toLocalDateTime()).build();
        playerRepository.save(creator);
        log.debug("Creator added as player with ID: {}", creator.getPlayerId());
//        publishMatchEvent(savedMatch, MatchEventType.MATCH_CREATED);

        return matchMapper.toMatchResponse(savedMatch);
    }

    @Override
    public MatchResponse updateMatch(UUID matchId, UpdateMatchRequest request) {
        log.debug("Updating match: {}", matchId);

        Match match = findMatchOrThrow(matchId);
        validateMatchUpdateable(match);
        matchMapper.updateMatchFromDto(request, match);
        match.setUpdatedAt(ZonedDateTime.now());

        Match updatedMatch = matchRepository.save(match);
        log.debug("Match updated with ID: {}", updatedMatch.getId());

        publishMatchEvent(updatedMatch, MatchEventType.MATCH_UPDATED);

        return matchMapper.toMatchResponse(updatedMatch);
    }

    @Override
    @Transactional(readOnly = true)
    public MatchResponse getMatch(UUID matchId) {
        log.debug("Fetching match: {}", matchId);
        Match match = findMatchOrThrow(matchId);
        return matchMapper.toMatchResponse(match);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MatchResponse> getMatches(Pageable pageable) {
        log.debug("Fetching matches with pageable: {}", pageable);
        return matchRepository.findAll(pageable).map(matchMapper::toMatchResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MatchResponse> getMatchesByCreator(UUID creatorId, Pageable pageable) {
        log.debug("Fetching matches for creator: {}", creatorId);
        return matchRepository.findByCreatorId(creatorId, pageable).map(matchMapper::toMatchResponse);
    }

    @Override
    public void deleteMatch(UUID matchId) {
        log.debug("Deleting match: {}", matchId);
        Match match = findMatchOrThrow(matchId);
        validateMatchDeletable(match);

        matchRepository.delete(match);
        log.debug("Match deleted with ID: {}", matchId);

        publishMatchEvent(match, MatchEventType.MATCH_DELETED);
    }

    @Override
    public MatchResponse updateMatchStatus(UUID matchId, MatchStatus newStatus) {
        log.debug("Updating status for match: {} to: {}", matchId, newStatus);
        Match match = findMatchOrThrow(matchId);
        validateStatusTransition(match.getStatus(), newStatus);

        match.setStatus(newStatus);
        match.setUpdatedAt(ZonedDateTime.now());

        Match updatedMatch = matchRepository.save(match);
        log.debug("Match status updated for ID: {} to {}", matchId, newStatus);

        publishMatchEvent(updatedMatch, MatchEventType.MATCH_STATUS_UPDATED);

        return matchMapper.toMatchResponse(updatedMatch);
    }
    private Match findMatchOrThrow(UUID matchId) {
        return matchRepository.findById(matchId).orElseThrow(() -> new org.fivy.matchservice.shared.exception.MatchException("Match not found with ID: " + matchId, "MATCH_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private void validateMatchUpdateable(Match match) {
        if (match.getStatus() != MatchStatus.DRAFT && match.getStatus() != MatchStatus.OPEN) {
            throw new InvalidMatchStateException("Match cannot be updated in status: " + match.getStatus());
        }
    }

    private void validateMatchDeletable(Match match) {
        if (match.getStatus() != MatchStatus.DRAFT && match.getStatus() != MatchStatus.OPEN) {
            throw new InvalidMatchStateException("Match cannot be deleted in status: " + match.getStatus());
        }
    }

    private void validateStatusTransition(MatchStatus currentStatus, MatchStatus newStatus) {
        switch (currentStatus) {
            case DRAFT:
                if (!(newStatus == MatchStatus.OPEN || newStatus == MatchStatus.CANCELLED)) {
                    throw new InvalidMatchStateException("Invalid status transition from DRAFT to " + newStatus);
                }
                break;
            case OPEN:
                if (!(newStatus == MatchStatus.IN_PROGRESS || newStatus == MatchStatus.CANCELLED)) {
                    throw new InvalidMatchStateException("Invalid status transition from OPEN to " + newStatus);
                }
                break;
            case IN_PROGRESS:
                if (!(newStatus == MatchStatus.COMPLETED || newStatus == MatchStatus.CANCELLED)) {
                    throw new InvalidMatchStateException("Invalid status transition from IN_PROGRESS to " + newStatus);
                }
                break;
            case COMPLETED:
            case CANCELLED:
                throw new InvalidMatchStateException("Cannot transition from final status: " + currentStatus);
            default:
                throw new InvalidMatchStateException("Cannot transition from status: " + currentStatus);
        }
    }

    private void publishMatchEvent(Match match, MatchEventType eventType) {
        log.debug("Publishing event: {} for match ID: {}", eventType, match.getId());

        MatchEvent event = MatchEvent.builder().type(eventType).matchId(match.getId()).creatorId(match.getCreatorId()).data(buildEventData(match)).timestamp(ZonedDateTime.now().toLocalDateTime()).build();

        kafkaTemplate.send("match-events", match.getId().toString(), event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish match event: {}", eventType, ex);
            } else {
                log.debug("Successfully published match event: {}", eventType);
            }
        });
    }

    private Map<String, String> buildEventData(Match match) {
        Map<String, String> data = new HashMap<>();
        data.put("title", match.getTitle());
        data.put("format", match.getFormat());
        data.put("status", match.getStatus().toString());
        data.put("address", match.getLocation().getAddress());
        data.put("latitude", match.getLocation().getCoordinates().getLatitude().toString());
        data.put("longitude", match.getLocation().getCoordinates().getLongitude().toString());
        data.put("skillLevel", match.getSkillLevel().name());
        data.put("startDate", match.getStartDate().toString());
        data.put("duration", match.getDuration().toString());
        data.put("creatorId", match.getCreatorId().toString());
        return data;
    }
}
