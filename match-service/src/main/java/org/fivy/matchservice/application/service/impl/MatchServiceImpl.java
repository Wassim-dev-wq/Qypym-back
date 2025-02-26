package org.fivy.matchservice.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.api.dto.request.CreateMatchRequest;
import org.fivy.matchservice.api.dto.request.UpdateMatchRequest;
import org.fivy.matchservice.api.dto.response.MatchDetailsResponse;
import org.fivy.matchservice.api.dto.response.MatchResponse;
import org.fivy.matchservice.api.mapper.MatchMapper;
import org.fivy.matchservice.application.service.MatchService;
import org.fivy.matchservice.domain.entity.Match;
import org.fivy.matchservice.domain.entity.MatchPlayer;
import org.fivy.matchservice.domain.entity.MatchTeam;
import org.fivy.matchservice.domain.entity.MatchWeather;
import org.fivy.matchservice.domain.enums.JoinRequestStatus;
import org.fivy.matchservice.domain.enums.MatchStatus;
import org.fivy.matchservice.domain.enums.SkillLevel;
import org.fivy.matchservice.domain.event.MatchEvent;
import org.fivy.matchservice.domain.event.MatchEventType;
import org.fivy.matchservice.domain.repository.*;
import org.fivy.matchservice.shared.exception.InvalidMatchStateException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MatchServiceImpl implements MatchService {
    private final MatchRepository matchRepository;
    private final MatchPlayerRepository playerRepository;
    private final MatchTeamRepository matchTeamRepository;
    private final SavedMatchRepository savedMatchRepository;
    private final MatchJoinRequestRepository matchJoinRequestRepository;
    private final MatchMapper matchMapper;
    private final WeatherServiceImpl weatherService;
    private final MatchWeatherRepository matchWeatherRepository;
    private final KafkaTemplate<String, MatchEvent> kafkaTemplate;

    @Override
    public MatchResponse createMatch(UUID creatorId, CreateMatchRequest request) {
        log.debug("Creating match for creator: {}", creatorId);
        Match match = matchMapper.toEntity(request);
        match.setCreatorId(creatorId);
        match.setStatus(MatchStatus.DRAFT);
        match.setCreatedAt(ZonedDateTime.now());
        match.setUpdatedAt(ZonedDateTime.now());
        match.setMaxPlayers(match.getMaxPlayers());
        match.setMaxPlayersPerTeam(match.getPlayersPerTeam());
        Match savedMatch = matchRepository.save(match);
        log.debug("Match created with ID: {}", savedMatch.getId());
        createTeamsForMatch(savedMatch);
        MatchWeather savedWeather = weatherService.fetchAndSaveWeather(savedMatch);
        if (savedWeather != null) {
            log.info("Saved weather data for match ID: {}", savedMatch.getId());
        }
        return matchMapper.toMatchResponse(savedMatch);
    }

    private void createTeamsForMatch(Match match) {
        log.debug("Creating teams for match: {} with format: {}", match.getId(), match.getFormat());

        MatchTeam team1 = MatchTeam.builder()
                .match(match)
                .teamNumber(1)
                .name("Team 1")
                .build();
        matchTeamRepository.save(team1);
        log.debug("Created team 1 with ID: {}", team1.getId());

        MatchTeam team2 = MatchTeam.builder()
                .match(match)
                .teamNumber(2)
                .name("Team 2")
                .build();
        matchTeamRepository.save(team2);
        log.debug("Created team 2 with ID: {}", team2.getId());

        log.info("Created {} teams for match ID: {} with format: {}", 2, match.getId(), match.getFormat());
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
    public MatchResponse getMatch(UUID matchId, UUID currentUserId) {
        log.debug("Fetching match: {}", matchId);
        Match match = findMatchOrThrow(matchId);
        MatchResponse response = matchMapper.toMatchResponse(match);
        response.setJoined(playerRepository.existsByMatchIdAndPlayerId(matchId, currentUserId));
        response.setMaxPlayers(match.getMaxPlayers());
        response.setJoinedCount(playerRepository.countByMatchId(matchId));
        response.setSavedCount(savedMatchRepository.countByMatchId(matchId));
        response.setJoinRequestCount(
                matchJoinRequestRepository.countByMatchIdAndRequestStatusNotIn(
                        match.getId(),
                        List.of(JoinRequestStatus.ACCEPTED, JoinRequestStatus.CANCELED)
                )
        );        response.setOwner(match.getCreatorId().equals(currentUserId));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public MatchDetailsResponse getMatchWithDetails(UUID matchId, UUID currentUserId) {
        log.debug("Fetching detailed match: {}", matchId);
        Match match = findMatchWithDetailsOrThrow(matchId);
        MatchDetailsResponse details = matchMapper.toDetailedMatchResponse(match);
        details.setOwner(match.getCreatorId().equals(currentUserId));
        details.setPlayers(mapPlayers(match));
        details.setTeams(mapTeams(match));
        details.setCurrentPlayers(match.getPlayers().size());
        if (match.getWeather() != null) {
            details.setWeather(matchMapper.toWeatherResponse(match.getWeather()));
        }
        log.info(details.toString());
        return details;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MatchResponse> getMatches(Pageable pageable) {
        return null;
    }

    private List<MatchDetailsResponse.MatchPlayerResponse> mapPlayers(Match match) {
        return match.getPlayers().stream()
                .map(matchMapper::toDetailedPlayerResponse)
                .collect(Collectors.toList());
    }

    private List<MatchDetailsResponse.MatchTeamResponse> mapTeams(Match match) {
        Map<UUID, List<MatchPlayer>> teamPlayersMap = createTeamPlayersMap(match);

        return match.getTeams().stream()
                .map(team -> buildTeamResponse(team, teamPlayersMap))
                .collect(Collectors.toList());
    }

    private Map<UUID, List<MatchPlayer>> createTeamPlayersMap(Match match) {
        return match.getPlayers().stream()
                .filter(player -> player.getTeam() != null)
                .collect(Collectors.groupingBy(
                        player -> player.getTeam().getId(),
                        Collectors.toList()
                ));
    }

    private MatchDetailsResponse.MatchTeamResponse buildTeamResponse(
            MatchTeam team,
            Map<UUID, List<MatchPlayer>> teamPlayersMap) {
        List<MatchPlayer> teamPlayers = teamPlayersMap.getOrDefault(team.getId(), Collections.emptyList());

        return MatchDetailsResponse.MatchTeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .teamNumber(team.getTeamNumber())
                .players(teamPlayers.stream()
                        .map(matchMapper::toDetailedPlayerResponse)
                        .collect(Collectors.toList()))
                .currentPlayers(teamPlayers.size())
                .maxPlayers(team.getMatch().getMaxPlayersPerTeam())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<MatchResponse> getMatches(
            Double latitude,
            Double longitude,
            Double distance,
            String skillLevel,
            Pageable pageable,
            UUID currentUserId
    ) {
        log.debug("Fetching matches with lat={}, lon={}, dist={}, skill={}",
                latitude, longitude, distance, skillLevel);
        Function<Match, MatchResponse> toResponseWithDetails = match -> {
            MatchResponse response = matchMapper.toMatchResponse(match);
            response.setOwner(match.getCreatorId().equals(currentUserId));
            response.setJoined(playerRepository.existsByMatchIdAndPlayerId(match.getId(), currentUserId));
            response.setMaxPlayers(match.getMaxPlayers());
            response.setJoinedCount(playerRepository.countByMatchId(match.getId()));
            response.setSavedCount(savedMatchRepository.countByMatchId(match.getId()));
            response.setJoinRequestCount(
                    matchJoinRequestRepository.countByMatchIdAndRequestStatusNotIn(
                            match.getId(),
                            List.of(JoinRequestStatus.ACCEPTED, JoinRequestStatus.CANCELED)
                    )
            );
            return response;
        };
        if (latitude != null && longitude != null && distance != null) {
            if (skillLevel != null && !skillLevel.isBlank()) {
                SkillLevel skillEnum = SkillLevel.valueOf(skillLevel.toUpperCase());
                return matchRepository.findAllWithinDistanceAndSkill(
                        latitude,
                        longitude,
                        distance,
                        skillEnum,
                        pageable
                ).map(toResponseWithDetails);
            } else {
                return matchRepository.findAllWithinDistance(
                        latitude,
                        longitude,
                        distance,
                        pageable
                ).map(toResponseWithDetails);
            }
        }
        if (skillLevel != null && !skillLevel.isBlank()) {
            SkillLevel skillEnum = SkillLevel.valueOf(skillLevel.toUpperCase());
            return matchRepository.findAllBySkillLevel(skillEnum, pageable)
                    .map(toResponseWithDetails);
        }
        return matchRepository.findAll(pageable)
                .map(toResponseWithDetails);
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

    private Match findMatchWithDetailsOrThrow(UUID matchId) {
        return matchRepository.findMatchWithDetails(matchId)
                .orElseThrow(() -> new org.fivy.matchservice.shared.exception.MatchException("Match with details not found with ID: " + matchId, "MATCH_NOT_FOUND", HttpStatus.NOT_FOUND));
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
        data.put("format", match.getFormat().toString());
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
