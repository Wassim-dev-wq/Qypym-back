package org.fivy.matchservice.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.api.dto.request.CreateMatchRequest;
import org.fivy.matchservice.api.dto.request.FilterMatchesRequest;
import org.fivy.matchservice.api.dto.request.UpdateMatchRequest;
import org.fivy.matchservice.api.dto.response.MatchDetailsResponse;
import org.fivy.matchservice.api.dto.response.MatchHistoryResponse;
import org.fivy.matchservice.api.dto.response.MatchResponse;
import org.fivy.matchservice.api.mapper.MatchMapper;
import org.fivy.matchservice.application.service.MatchService;
import org.fivy.matchservice.domain.entity.*;
import org.fivy.matchservice.domain.enums.*;
import org.fivy.matchservice.domain.event.MatchEvent;
import org.fivy.matchservice.domain.event.MatchEventType;
import org.fivy.matchservice.domain.repository.*;
import org.fivy.matchservice.infrastructure.config.kafka.KafkaConfig;
import org.fivy.matchservice.shared.exception.InvalidMatchStateException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final MatchResultRepository matchResultRepository;
    private final MatchScoreSubmissionRepository scoreSubmissionRepository;
    private final MatchFeedbackRequestRepository feedbackRequestRepository;
    private final PlayerMatchFeedbackRepository playerFeedbackRepository;
    private final PlayerRatingRepository playerRatingRepository;
    private final KafkaTemplate<String, MatchEvent> kafkaTemplate;
    private final NotificationService notificationService;

    @Override
    public MatchResponse createMatch(UUID creatorId, CreateMatchRequest request) {
        log.debug("Creating match for creator: {}", creatorId);
        Match match = matchMapper.toEntity(request);
        match.setCreatorId(creatorId);
        match.setStatus(MatchStatus.OPEN);
        match.setCreatedAt(ZonedDateTime.now());
        match.setUpdatedAt(ZonedDateTime.now());
        match.setMaxPlayers(match.getMaxPlayers());
        match.setMaxPlayersPerTeam(match.getPlayersPerTeam());
        Match savedMatch = matchRepository.save(match);
        log.debug("Match created with ID: {}", savedMatch.getId());
        MatchTeam team1 = createTeamsForMatch(savedMatch);
        assignCreatorToATeam(creatorId, savedMatch, team1);
        MatchWeather savedWeather = weatherService.fetchAndSaveWeather(savedMatch);
        if (savedWeather != null) {
            log.info("Saved weather data for match ID: {}", savedMatch.getId());
        }
        publishMatchEvent(savedMatch, MatchEventType.MATCH_CREATED);
        return matchMapper.toMatchResponse(savedMatch);
    }

    private void assignCreatorToATeam(UUID creatorId, Match savedMatch, MatchTeam team1) {
        PlayerRole role = PlayerRole.MIDFIELDER;
        MatchPlayer newPlayer = MatchPlayer.builder()
                .match(savedMatch)
                .team(team1)
                .playerId(creatorId)
                .role(role)
                .status(PlayerStatus.JOINED)
                .joinedAt(LocalDateTime.now())
                .build();
        playerRepository.save(newPlayer);
        log.info("Match creator has joined team {}", team1.getId());
    }

    private MatchTeam createTeamsForMatch(Match match) {
        log.debug("Creating teams for match: {} with format: {}", match.getId(), match.getFormat());

        MatchTeam team1 = MatchTeam.builder()
                .match(match)
                .teamNumber(1)
                .name("Équipe 1")
                .build();
        matchTeamRepository.save(team1);
        log.debug("Created team 1 with ID: {}", team1.getId());

        MatchTeam team2 = MatchTeam.builder()
                .match(match)
                .teamNumber(2)
                .name("Équipe 2")
                .build();
        matchTeamRepository.save(team2);
        log.debug("Created team 2 with ID: {}", team2.getId());

        log.info("Created {} teams for match ID: {} with format: {}", 2, match.getId(), match.getFormat());
        return team1;
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
                        List.of(JoinRequestStatus.ACCEPTED, JoinRequestStatus.CANCELED,  JoinRequestStatus.LEFT)
                )
        );
        response.setOwner(match.getCreatorId().equals(currentUserId));
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
                .filter(player -> player.getTeam() != null && player.getStatus() != PlayerStatus.LEFT)
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
                            List.of(JoinRequestStatus.ACCEPTED, JoinRequestStatus.CANCELED, JoinRequestStatus.LEFT)
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
    public Page<MatchResponse> searchMatches(FilterMatchesRequest filters, Pageable pageable, UUID currentUserId) {
        if (filters == null) {
            throw new IllegalArgumentException("Filter request cannot be null");
        }
        if (currentUserId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        log.debug("Searching matches with filters: {}", filters);
        String searchQuery = filters.getSearchQuery();
        List<SkillLevel> skillLevelEnums = filters.getSkillLevels() != null ?
                filters.getSkillLevels() : new ArrayList<>();

        List<MatchStatus> statusEnums = filters.getStatuses() != null ?
                filters.getStatuses() : new ArrayList<>();

        List<MatchFormat> formatEnums = filters.getFormats() != null ?
                filters.getFormats() : new ArrayList<>();
        List<String> skillLevels = skillLevelEnums.stream()
                .map(Enum::name)
                .collect(Collectors.toList());

        List<String> statuses = statusEnums.stream()
                .map(Enum::name)
                .collect(Collectors.toList());

        List<String> formats = formatEnums.stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        Double latitude = filters.getLatitude();
        Double longitude = filters.getLongitude();
        Double distance = filters.getDistance();
        Page<Match> matchPage = matchRepository.findAllByFilters(
                searchQuery,
                skillLevels,
                skillLevels.isEmpty(),
                statuses,
                statuses.isEmpty(),
                formats,
                formats.isEmpty(),
                latitude,
                longitude,
                distance,
                pageable
        );

        return matchPage.map(match -> {
            MatchResponse response = matchMapper.toMatchResponse(match);
            response.setJoined(playerRepository.existsByMatchIdAndPlayerId(match.getId(), currentUserId));
            response.setOwner(match.getCreatorId().equals(currentUserId));
            response.setJoinedCount(playerRepository.countByMatchId(match.getId()));
            response.setSavedCount(savedMatchRepository.countByMatchId(match.getId()));
            response.setJoinRequestCount(
                    matchJoinRequestRepository.countByMatchIdAndRequestStatusNotIn(
                            match.getId(),
                            List.of(JoinRequestStatus.ACCEPTED, JoinRequestStatus.CANCELED,  JoinRequestStatus.LEFT)
                    )
            );
            return response;

        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MatchResponse> getUserUpcomingMatches(UUID userId, Pageable pageable) {
        log.debug("Fetching upcoming matches for user: {}", userId);
        ZonedDateTime now = ZonedDateTime.now();
        Page<Match> upcomingMatches = matchRepository.findUserUpcomingMatches(
                userId,
                now,
                Arrays.asList(MatchStatus.OPEN, MatchStatus.IN_PROGRESS),
                PlayerStatus.LEFT,
                pageable
        );
        return upcomingMatches.map(match -> {
            MatchResponse response = matchMapper.toMatchResponse(match);
            response.setJoined(true);
            response.setOwner(match.getCreatorId().equals(userId));
            response.setMaxPlayers(match.getMaxPlayers());
            response.setJoinedCount(playerRepository.countByMatchId(match.getId()));
            response.setSavedCount(savedMatchRepository.countByMatchId(match.getId()));
            response.setJoinRequestCount(
                    matchJoinRequestRepository.countByMatchIdAndRequestStatusNotIn(
                            match.getId(),
                            List.of(JoinRequestStatus.ACCEPTED, JoinRequestStatus.CANCELED, JoinRequestStatus.LEFT)
                    )
            );
            return response;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public MatchHistoryResponse getMatchHistoryDetail(UUID matchId, UUID userId) {
        log.debug("Fetching match history detail for matchId: {} and userId: {}", matchId, userId);

        Match match = findMatchOrThrow(matchId);

        MatchPlayer matchPlayer = playerRepository.findByMatchIdAndPlayerId(matchId, userId)
                .orElseThrow(() -> new org.fivy.matchservice.shared.exception.MatchException(
                        "User did not participate in this match: " + matchId,
                        "USER_NOT_IN_MATCH",
                        HttpStatus.FORBIDDEN));

        MatchHistoryResponse response = matchMapper.toMatchHistoryResponse(match);
        response.setIsCreator(match.getCreatorId().equals(userId));

        response.setPlayerRole(matchPlayer.getRole());
        response.setPlayerStatus(matchPlayer.getStatus());
        response.setJoinedAt(matchPlayer.getJoinedAt());

        if (matchPlayer.getTeam() != null) {
            response.setPlayerTeamId(matchPlayer.getTeam().getId());
            response.setPlayerTeamName(matchPlayer.getTeam().getName());
            response.setPlayerTeamNumber(matchPlayer.getTeam().getTeamNumber());
        }

        if (match.getWeather() != null) {
            MatchWeather weather = match.getWeather();
            response.setWeather(MatchHistoryResponse.WeatherInfo.builder()
                    .temperature(weather.getTemperature())
                    .condition(weather.getCondition())
                    .humidity(weather.getHumidity())
                    .windSpeed(weather.getWindSpeed())
                    .cloudCoverage(weather.getCloudCoverage())
                    .build());
        }

        List<MatchTeam> teams = matchTeamRepository.findByMatchId(match.getId());
        List<MatchHistoryResponse.TeamInfo> teamInfos = new ArrayList<>();

        for (MatchTeam team : teams) {
            List<MatchPlayer> teamPlayers = playerRepository.findByTeamId(team.getId());
            List<MatchHistoryResponse.PlayerInfo> playerInfos = teamPlayers.stream()
                    .map(player -> MatchHistoryResponse.PlayerInfo.builder()
                            .playerId(player.getPlayerId())
                            .role(player.getRole())
                            .status(player.getStatus())
                            .joinedAt(player.getJoinedAt())
                            .build())
                    .collect(Collectors.toList());

            teamInfos.add(MatchHistoryResponse.TeamInfo.builder()
                    .teamId(team.getId())
                    .teamName(team.getName())
                    .teamNumber(team.getTeamNumber())
                    .players(playerInfos)
                    .build());
        }
        response.setTeams(teamInfos);

        matchResultRepository.findByMatchId(match.getId()).ifPresent(result -> {
            MatchHistoryResponse.ResultInfo resultInfo = MatchHistoryResponse.ResultInfo.builder()
                    .status(result.getStatus())
                    .team1Score(result.getTeam1Score())
                    .team2Score(result.getTeam2Score())
                    .confirmedAt(result.getConfirmedAt())
                    .build();

            if (result.getWinningTeam() != null) {
                resultInfo.setWinningTeamId(result.getWinningTeam().getId());
                resultInfo.setWinningTeamName(result.getWinningTeam().getName());
                resultInfo.setWinningTeamNumber(result.getWinningTeam().getTeamNumber());
            }

            scoreSubmissionRepository.findByMatchIdAndSubmitterId(match.getId(), userId)
                    .ifPresent(submission -> {
                        resultInfo.setUserSubmittedScore(true);
                        resultInfo.setUserSubmittedTeam1Score(submission.getTeam1Score());
                        resultInfo.setUserSubmittedTeam2Score(submission.getTeam2Score());
                        resultInfo.setUserScoreSubmissionStatus(submission.getStatus());
                    });

            response.setResult(resultInfo);
        });

        feedbackRequestRepository.findByMatchId(match.getId()).ifPresent(feedbackRequest -> {
            MatchHistoryResponse.FeedbackInfo feedbackInfo = MatchHistoryResponse.FeedbackInfo.builder()
                    .requestStatus(feedbackRequest.getStatus())
                    .expiryAt(feedbackRequest.getExpiryAt())
                    .build();

            playerFeedbackRepository.findByFeedbackRequestIdAndPlayerId(feedbackRequest.getId(), userId)
                    .ifPresent(feedback -> {
                        feedbackInfo.setUserSubmittedFeedback(true);
                        feedbackInfo.setUserMatchRating(feedback.getMatchRating());
                        feedbackInfo.setUserMatchComments(feedback.getMatchComments());
                        feedbackInfo.setUserFeedbackSubmittedAt(feedback.getSubmittedAt());
                    });

            List<PlayerRating> ratingsReceived = playerRatingRepository.findByFeedbackRequestIdAndRatedPlayerId(
                    feedbackRequest.getId(), userId);

            if (!ratingsReceived.isEmpty()) {
                List<MatchHistoryResponse.RatingInfo> ratings = ratingsReceived.stream()
                        .map(rating -> MatchHistoryResponse.RatingInfo.builder()
                                .ratingPlayerId(rating.getRatingPlayerId())
                                .skillRating(rating.getSkillRating())
                                .sportsmanshipRating(rating.getSportsmanshipRating())
                                .teamworkRating(rating.getTeamworkRating())
                                .reliabilityRating(rating.getReliabilityRating())
                                .comments(rating.getComments())
                                .build())
                        .collect(Collectors.toList());

                feedbackInfo.setRatingsReceived(ratings);

                feedbackInfo.setAvgSkillRating(ratingsReceived.stream()
                        .filter(r -> r.getSkillRating() != null)
                        .mapToInt(PlayerRating::getSkillRating)
                        .average().orElse(0));
                feedbackInfo.setAvgSportsmanshipRating(ratingsReceived.stream()
                        .filter(r -> r.getSportsmanshipRating() != null)
                        .mapToInt(PlayerRating::getSportsmanshipRating)
                        .average().orElse(0));
                feedbackInfo.setAvgTeamworkRating(ratingsReceived.stream()
                        .filter(r -> r.getTeamworkRating() != null)
                        .mapToInt(PlayerRating::getTeamworkRating)
                        .average().orElse(0));
                feedbackInfo.setAvgReliabilityRating(ratingsReceived.stream()
                        .filter(r -> r.getReliabilityRating() != null)
                        .mapToInt(PlayerRating::getReliabilityRating)
                        .average().orElse(0));

                double overallAvg = (feedbackInfo.getAvgSkillRating() +
                        feedbackInfo.getAvgSportsmanshipRating() +
                        feedbackInfo.getAvgTeamworkRating() +
                        feedbackInfo.getAvgReliabilityRating()) / 4.0;
                feedbackInfo.setAvgOverallRating(overallAvg);
            }

            response.setFeedback(feedbackInfo);
        });

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MatchHistoryResponse> getMatchHistory(UUID userId, Pageable pageable) {
        log.debug("Fetching match history for user: {}", userId);

        Page<MatchPlayer> userMatchPlayers = playerRepository.findByPlayerIdAndMatchStatusOrderByMatchStartDateDesc(userId, MatchStatus.FINISHED, pageable);

        return userMatchPlayers.map(matchPlayer -> {
            Match match = matchPlayer.getMatch();

            MatchHistoryResponse response = matchMapper.toMatchHistoryResponse(match);
            response.setIsCreator(match.getCreatorId().equals(userId));

            response.setPlayerRole(matchPlayer.getRole());
            response.setPlayerStatus(matchPlayer.getStatus());
            response.setJoinedAt(matchPlayer.getJoinedAt());

            if (matchPlayer.getTeam() != null) {
                response.setPlayerTeamId(matchPlayer.getTeam().getId());
                response.setPlayerTeamName(matchPlayer.getTeam().getName());
                response.setPlayerTeamNumber(matchPlayer.getTeam().getTeamNumber());
            }

            if (match.getWeather() != null) {
                MatchWeather weather = match.getWeather();
                response.setWeather(MatchHistoryResponse.WeatherInfo.builder()
                        .temperature(weather.getTemperature())
                        .condition(weather.getCondition())
                        .humidity(weather.getHumidity())
                        .windSpeed(weather.getWindSpeed())
                        .cloudCoverage(weather.getCloudCoverage())
                        .build());
            }

            List<MatchTeam> teams = matchTeamRepository.findByMatchId(match.getId());
            List<MatchHistoryResponse.TeamInfo> teamInfos = new ArrayList<>();

            for (MatchTeam team : teams) {
                List<MatchPlayer> teamPlayers = playerRepository.findByTeamId(team.getId());
                List<MatchHistoryResponse.PlayerInfo> playerInfos = teamPlayers.stream()
                        .map(player -> MatchHistoryResponse.PlayerInfo.builder()
                                .playerId(player.getPlayerId())
                                .role(player.getRole())
                                .status(player.getStatus())
                                .joinedAt(player.getJoinedAt())
                                .build())
                        .collect(Collectors.toList());

                teamInfos.add(MatchHistoryResponse.TeamInfo.builder()
                        .teamId(team.getId())
                        .teamName(team.getName())
                        .teamNumber(team.getTeamNumber())
                        .players(playerInfos)
                        .build());
            }
            response.setTeams(teamInfos);

            matchResultRepository.findByMatchId(match.getId()).ifPresent(result -> {
                MatchHistoryResponse.ResultInfo resultInfo = MatchHistoryResponse.ResultInfo.builder()
                        .status(result.getStatus())
                        .team1Score(result.getTeam1Score())
                        .team2Score(result.getTeam2Score())
                        .confirmedAt(result.getConfirmedAt())
                        .build();

                if (result.getWinningTeam() != null) {
                    resultInfo.setWinningTeamId(result.getWinningTeam().getId());
                    resultInfo.setWinningTeamName(result.getWinningTeam().getName());
                    resultInfo.setWinningTeamNumber(result.getWinningTeam().getTeamNumber());
                }

                scoreSubmissionRepository.findByMatchIdAndSubmitterId(match.getId(), userId)
                        .ifPresent(submission -> {
                            resultInfo.setUserSubmittedScore(true);
                            resultInfo.setUserSubmittedTeam1Score(submission.getTeam1Score());
                            resultInfo.setUserSubmittedTeam2Score(submission.getTeam2Score());
                            resultInfo.setUserScoreSubmissionStatus(submission.getStatus());
                        });

                response.setResult(resultInfo);
            });

            Optional<MatchFeedbackRequest> feedbackRequestOpt = feedbackRequestRepository.findByMatchId(match.getId());
            feedbackRequestOpt.ifPresent(feedbackRequest -> {
                MatchHistoryResponse.FeedbackInfo feedbackInfo = MatchHistoryResponse.FeedbackInfo.builder()
                        .requestStatus(feedbackRequest.getStatus())
                        .expiryAt(feedbackRequest.getExpiryAt())
                        .build();

                playerFeedbackRepository.findByFeedbackRequestIdAndPlayerId(feedbackRequest.getId(), userId)
                        .ifPresent(feedback -> {
                            feedbackInfo.setUserSubmittedFeedback(true);
                            feedbackInfo.setUserMatchRating(feedback.getMatchRating());
                            feedbackInfo.setUserMatchComments(feedback.getMatchComments());
                            feedbackInfo.setUserFeedbackSubmittedAt(feedback.getSubmittedAt());
                        });

                List<PlayerRating> ratingsReceived = playerRatingRepository.findByFeedbackRequestIdAndRatedPlayerId(
                        feedbackRequest.getId(), userId);

                if (!ratingsReceived.isEmpty()) {
                    List<MatchHistoryResponse.RatingInfo> ratings = ratingsReceived.stream()
                            .map(rating -> MatchHistoryResponse.RatingInfo.builder()
                                    .ratingPlayerId(rating.getRatingPlayerId())
                                    .skillRating(rating.getSkillRating())
                                    .sportsmanshipRating(rating.getSportsmanshipRating())
                                    .teamworkRating(rating.getTeamworkRating())
                                    .reliabilityRating(rating.getReliabilityRating())
                                    .comments(rating.getComments())
                                    .build())
                            .collect(Collectors.toList());

                    feedbackInfo.setRatingsReceived(ratings);

                    feedbackInfo.setAvgSkillRating(ratingsReceived.stream()
                            .filter(r -> r.getSkillRating() != null)
                            .mapToInt(PlayerRating::getSkillRating)
                            .average().orElse(0));
                    feedbackInfo.setAvgSportsmanshipRating(ratingsReceived.stream()
                            .filter(r -> r.getSportsmanshipRating() != null)
                            .mapToInt(PlayerRating::getSportsmanshipRating)
                            .average().orElse(0));
                    feedbackInfo.setAvgTeamworkRating(ratingsReceived.stream()
                            .filter(r -> r.getTeamworkRating() != null)
                            .mapToInt(PlayerRating::getTeamworkRating)
                            .average().orElse(0));
                    feedbackInfo.setAvgReliabilityRating(ratingsReceived.stream()
                            .filter(r -> r.getReliabilityRating() != null)
                            .mapToInt(PlayerRating::getReliabilityRating)
                            .average().orElse(0));

                    double overallAvg = (feedbackInfo.getAvgSkillRating() +
                            feedbackInfo.getAvgSportsmanshipRating() +
                            feedbackInfo.getAvgTeamworkRating() +
                            feedbackInfo.getAvgReliabilityRating()) / 4.0;
                    feedbackInfo.setAvgOverallRating(overallAvg);
                }
                response.setFeedback(feedbackInfo);
            });

            return response;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MatchResponse> getMatchesByCreator(UUID creatorId, Pageable pageable) {
        log.debug("Fetching matches for creator: {}", creatorId);
        Page<Match> matchPage = matchRepository.findByCreatorId(creatorId, pageable);
        return matchPage.map(match -> {
            MatchResponse response = matchMapper.toMatchResponse(match);
            response.setOwner(match.getCreatorId().equals(creatorId));
            response.setCreatorId(creatorId);
            response.setJoined(playerRepository.existsByMatchIdAndPlayerId(match.getId(), creatorId));
            response.setMaxPlayers(match.getMaxPlayers());
            response.setJoinedCount(playerRepository.countByMatchId(match.getId()));
            response.setSavedCount(savedMatchRepository.countByMatchId(match.getId()));
            response.setJoinRequestCount(
                    matchJoinRequestRepository.countByMatchIdAndRequestStatusNotIn(
                            match.getId(),
                            List.of(JoinRequestStatus.ACCEPTED, JoinRequestStatus.CANCELED, JoinRequestStatus.LEFT)
                    )
            );
            return response;
        });
    }

    @Override
    public void deleteMatch(UUID matchId) {
        log.debug("Deleting match: {}", matchId);
        Match match = findMatchOrThrow(matchId);
        validateMatchDeletable(match);

        publishMatchEvent(match, MatchEventType.MATCH_DELETED);

        log.debug("Deleting dependent entities for match: {}", matchId);
        playerRatingRepository.deleteByFeedbackFeedbackRequestIdIn(
                feedbackRequestRepository.findByMatchId(matchId)
                        .map(MatchFeedbackRequest::getId)
                        .stream()
                        .collect(Collectors.toList())
        );
        playerFeedbackRepository.deleteByFeedbackRequestIdIn(
                feedbackRequestRepository.findByMatchId(matchId)
                        .map(MatchFeedbackRequest::getId)
                        .stream()
                        .collect(Collectors.toList())
        );
        feedbackRequestRepository.deleteByMatchId(matchId);
        scoreSubmissionRepository.deleteByMatchId(matchId);
        matchResultRepository.deleteByMatchId(matchId);
        matchJoinRequestRepository.deleteByMatchId(matchId);
        playerRepository.deleteByMatchId(matchId);
        matchTeamRepository.deleteByMatchId(matchId);
        weatherService.deleteWeatherByMatchId(matchId);
        savedMatchRepository.deleteByMatchId(matchId);

        matchRepository.delete(match);
        log.info("Match and all dependent entities deleted for match ID: {}", matchId);
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
        log.debug("Publication de l'événement: {} pour le match ID: {}", eventType, match.getId());
        Map<String, String> eventData = buildEventData(match);
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
                        log.error("Échec de la publication de l'événement: {}", eventType, ex);
                    } else {
                        log.debug("Événement publié avec succès: {}", eventType);

                        notificationService.sendMatchEventNotifications(match, eventType);
                    }
                });
    }

    private Map<String, String> buildEventData(Match match) {
        Map<String, String> data = new HashMap<>();
        data.put("title", match.getTitle());
        data.put("format", match.getFormat().toString());
        data.put("status", match.getStatus().toString());
        if (match.getLocation() != null) {
            data.put("address", match.getLocation().getAddress());
            if (match.getLocation().getCoordinates() != null) {
                data.put("latitude", match.getLocation().getCoordinates().getLatitude().toString());
                data.put("longitude", match.getLocation().getCoordinates().getLongitude().toString());
            }
        }
        data.put("skillLevel", match.getSkillLevel().name());
        data.put("startDate", match.getStartDate().toString());
        data.put("duration", match.getDuration().toString());
        data.put("creatorId", match.getCreatorId().toString());
        return data;
    }

}
