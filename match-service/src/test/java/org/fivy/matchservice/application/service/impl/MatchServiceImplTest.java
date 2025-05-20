package org.fivy.matchservice.application.service.impl;

import org.fivy.matchservice.api.dto.request.CreateMatchRequest;
import org.fivy.matchservice.api.dto.request.FilterMatchesRequest;
import org.fivy.matchservice.api.dto.request.UpdateMatchRequest;
import org.fivy.matchservice.api.dto.response.MatchDetailsResponse;
import org.fivy.matchservice.api.dto.response.MatchHistoryResponse;
import org.fivy.matchservice.api.dto.response.MatchResponse;
import org.fivy.matchservice.api.mapper.MatchMapper;
import org.fivy.matchservice.domain.entity.Match;
import org.fivy.matchservice.domain.entity.MatchPlayer;
import org.fivy.matchservice.domain.entity.MatchTeam;
import org.fivy.matchservice.domain.enums.*;
import org.fivy.matchservice.domain.event.MatchEvent;
import org.fivy.matchservice.domain.repository.*;
import org.fivy.matchservice.shared.exception.InvalidMatchStateException;
import org.fivy.matchservice.shared.exception.MatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MatchServiceImplTest {

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MatchPlayerRepository playerRepository;
    @Mock
    private MatchTeamRepository matchTeamRepository;
    @Mock
    private SavedMatchRepository savedMatchRepository;
    @Mock
    private MatchJoinRequestRepository matchJoinRequestRepository;
    @Mock
    private MatchMapper matchMapper;
    @Mock
    private WeatherServiceImpl weatherService;
    @Mock
    private MatchResultRepository matchResultRepository;
    @Mock
    private MatchScoreSubmissionRepository scoreSubmissionRepository;
    @Mock
    private MatchFeedbackRequestRepository feedbackRequestRepository;
    @Mock
    private PlayerMatchFeedbackRepository playerFeedbackRepository;
    @Mock
    private PlayerRatingRepository playerRatingRepository;
    @Mock
    private KafkaTemplate<String, MatchEvent> kafkaTemplate;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserBlockingService userBlockingService;

    @InjectMocks
    private MatchServiceImpl matchService;

    private UUID creatorId;
    private UUID matchId;
    private UUID teamId;
    private Match match;
    private MatchTeam team1;
    private MatchTeam team2;
    private MatchPlayer matchPlayer;
    private MatchResponse matchResponse;
    private MatchDetailsResponse matchDetailsResponse;
    private CreateMatchRequest createMatchRequest;
    private UpdateMatchRequest updateMatchRequest;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        creatorId = UUID.randomUUID();
        matchId = UUID.randomUUID();
        teamId = UUID.randomUUID();


        match = new Match();
        match.setId(matchId);
        match.setCreatorId(creatorId);
        match.setTitle("Test Match");
        match.setStatus(MatchStatus.OPEN);
        match.setFormat(MatchFormat.FIVE_V_FIVE);
        match.setSkillLevel(SkillLevel.INTERMEDIATE);
        match.setStartDate(ZonedDateTime.now().plusDays(1));
        match.setDuration(120);
        match.setMaxPlayers(10);
        match.setMaxPlayersPerTeam(5);
        match.setCreatedAt(ZonedDateTime.now());
        match.setUpdatedAt(ZonedDateTime.now());


        team1 = new MatchTeam();
        team1.setId(UUID.randomUUID());
        team1.setMatch(match);
        team1.setTeamNumber(1);
        team1.setName("Équipe 1");

        team2 = new MatchTeam();
        team2.setId(UUID.randomUUID());
        team2.setMatch(match);
        team2.setTeamNumber(2);
        team2.setName("Équipe 2");

        Set<MatchTeam> teams = new HashSet<>(Arrays.asList(team1, team2));
        match.setTeams(teams);


        matchPlayer = new MatchPlayer();
        matchPlayer.setId(UUID.randomUUID());
        matchPlayer.setMatch(match);
        matchPlayer.setTeam(team1);
        matchPlayer.setPlayerId(creatorId);
        matchPlayer.setRole(PlayerRole.MIDFIELDER);
        matchPlayer.setStatus(PlayerStatus.JOINED);
        matchPlayer.setJoinedAt(LocalDateTime.now());

        Set<MatchPlayer> players = new HashSet<>(Collections.singletonList(matchPlayer));
        match.setPlayers(players);


        matchResponse = new MatchResponse();
        matchResponse.setId(matchId);
        matchResponse.setCreatorId(creatorId);
        matchResponse.setTitle("Test Match");
        matchResponse.setStatus(MatchStatus.OPEN);


        matchDetailsResponse = new MatchDetailsResponse();
        matchDetailsResponse.setId(matchId);
        matchDetailsResponse.setCreatorId(creatorId);
        matchDetailsResponse.setTitle("Test Match");
        matchDetailsResponse.setStatus(MatchStatus.OPEN);


        createMatchRequest = new CreateMatchRequest();
        createMatchRequest.setTitle("New Test Match");
        createMatchRequest.setFormat(MatchFormat.FIVE_V_FIVE);
        createMatchRequest.setSkillLevel(SkillLevel.INTERMEDIATE);


        updateMatchRequest = new UpdateMatchRequest();
        updateMatchRequest.setTitle("Updated Test Match");


        pageable = Pageable.ofSize(10);


        lenient().when(kafkaTemplate.send(anyString(), anyString(), any(MatchEvent.class)))
                .thenReturn(mock(CompletableFuture.class));
    }

    @Test
    void createMatch_Success() {

        when(matchMapper.toEntity(any(CreateMatchRequest.class))).thenReturn(match);
        when(matchRepository.save(any(Match.class))).thenReturn(match);
        when(matchTeamRepository.save(any(MatchTeam.class))).thenReturn(team1).thenReturn(team2);
        when(playerRepository.save(any(MatchPlayer.class))).thenReturn(matchPlayer);
        when(matchMapper.toMatchResponse(any(Match.class))).thenReturn(matchResponse);


        MatchResponse result = matchService.createMatch(creatorId, createMatchRequest);


        assertNotNull(result);
        assertEquals(matchId, result.getId());


        verify(matchMapper).toEntity(createMatchRequest);
        verify(matchRepository).save(any(Match.class));
        verify(matchTeamRepository, times(2)).save(any(MatchTeam.class));
        verify(playerRepository).save(any(MatchPlayer.class));
        verify(matchMapper).toMatchResponse(match);
        verify(kafkaTemplate).send(anyString(), eq(matchId.toString()), any(MatchEvent.class));
        verify(weatherService).fetchAndSaveWeather(match);
    }

    @Test
    void updateMatch_Success() {

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchRepository.save(any(Match.class))).thenReturn(match);
        when(matchMapper.toMatchResponse(any(Match.class))).thenReturn(matchResponse);


        MatchResponse result = matchService.updateMatch(matchId, updateMatchRequest);


        assertNotNull(result);
        assertEquals(matchId, result.getId());


        verify(matchRepository).findById(matchId);
        verify(matchMapper).updateMatchFromDto(updateMatchRequest, match);
        verify(matchRepository).save(match);
        verify(matchMapper).toMatchResponse(match);
        verify(kafkaTemplate).send(anyString(), eq(matchId.toString()), any(MatchEvent.class));
    }

    @Test
    void updateMatch_MatchNotFound() {

        when(matchRepository.findById(matchId)).thenReturn(Optional.empty());


        MatchException exception = assertThrows(MatchException.class, () ->
                matchService.updateMatch(matchId, updateMatchRequest));

        assertEquals("Match not found with ID: " + matchId, exception.getMessage());
        assertEquals("MATCH_NOT_FOUND", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void updateMatch_InvalidState() {

        match.setStatus(MatchStatus.IN_PROGRESS);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));


        InvalidMatchStateException exception = assertThrows(InvalidMatchStateException.class, () ->
                matchService.updateMatch(matchId, updateMatchRequest));

        assertEquals("Match cannot be updated in status: " + MatchStatus.IN_PROGRESS, exception.getMessage());
    }

    @Test
    void getMatch_Success() {

        String authToken = "test-token";
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(playerRepository.existsByMatchIdAndPlayerId(matchId, creatorId)).thenReturn(true);
        when(playerRepository.countByMatchId(matchId)).thenReturn(1);
        when(savedMatchRepository.countByMatchId(matchId)).thenReturn(0);
        when(matchJoinRequestRepository.countByMatchIdAndRequestStatusNotIn(
                eq(matchId), anyList())).thenReturn(0);
        when(matchMapper.toMatchResponse(any(Match.class))).thenReturn(matchResponse);


        MatchResponse result = matchService.getMatch(matchId, creatorId, authToken);


        assertNotNull(result);
        assertEquals(matchId, result.getId());


        verify(matchRepository).findById(matchId);
        verify(playerRepository).existsByMatchIdAndPlayerId(matchId, creatorId);
        verify(playerRepository).countByMatchId(matchId);
        verify(savedMatchRepository).countByMatchId(matchId);
        verify(matchJoinRequestRepository).countByMatchIdAndRequestStatusNotIn(
                eq(matchId), anyList());
        verify(matchMapper).toMatchResponse(match);
    }

    @Test
    void getMatchWithDetails_Success() {

        when(matchRepository.findMatchWithDetails(matchId)).thenReturn(Optional.of(match));
        when(matchMapper.toDetailedMatchResponse(any(Match.class))).thenReturn(matchDetailsResponse);
        when(matchMapper.toDetailedPlayerResponse(any(MatchPlayer.class)))
                .thenReturn(new MatchDetailsResponse.MatchPlayerResponse());


        MatchDetailsResponse result = matchService.getMatchWithDetails(matchId, creatorId);


        assertNotNull(result);
        assertEquals(matchId, result.getId());


        verify(matchRepository).findMatchWithDetails(matchId);
        verify(matchMapper).toDetailedMatchResponse(match);
        verify(matchMapper, atLeastOnce()).toDetailedPlayerResponse(any(MatchPlayer.class));
    }

    @Test
    void getMatches_Success() {

        Double latitude = 48.8566;
        Double longitude = 2.3522;
        Double distance = 10.0;
        String skillLevel = "INTERMEDIATE";
        String authToken = "test-token";
        List<UUID> blockedUserIds = Collections.emptyList();

        Page<Match> matchPage = new PageImpl<>(Collections.singletonList(match));

        when(userBlockingService.getBlockedUserIds(anyString())).thenReturn(blockedUserIds);
        when(matchRepository.findAllWithinDistanceAndSkill(
                eq(latitude), eq(longitude), eq(distance), any(SkillLevel.class), eq(pageable)))
                .thenReturn(matchPage);
        when(playerRepository.existsByMatchIdAndPlayerId(matchId, creatorId)).thenReturn(true);
        when(playerRepository.countByMatchId(matchId)).thenReturn(1);
        when(savedMatchRepository.countByMatchId(matchId)).thenReturn(0);
        when(matchJoinRequestRepository.countByMatchIdAndRequestStatusNotIn(
                eq(matchId), anyList())).thenReturn(0);
        when(matchMapper.toMatchResponse(any(Match.class))).thenReturn(matchResponse);


        Page<MatchResponse> result = matchService.getMatches(
                latitude, longitude, distance, skillLevel, pageable, creatorId, authToken);


        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(matchId, result.getContent().get(0).getId());


        verify(userBlockingService).getBlockedUserIds(anyString());
        verify(matchRepository).findAllWithinDistanceAndSkill(
                eq(latitude), eq(longitude), eq(distance), any(SkillLevel.class), eq(pageable));
        verify(playerRepository).existsByMatchIdAndPlayerId(matchId, creatorId);
        verify(playerRepository).countByMatchId(matchId);
        verify(savedMatchRepository).countByMatchId(matchId);
        verify(matchJoinRequestRepository).countByMatchIdAndRequestStatusNotIn(
                eq(matchId), anyList());
        verify(matchMapper).toMatchResponse(match);
    }

    @Test
    void searchMatches_Success() {

        FilterMatchesRequest filters = new FilterMatchesRequest();
        filters.setSearchQuery("test");
        filters.setSkillLevels(Collections.singletonList(SkillLevel.INTERMEDIATE));
        filters.setStatuses(Collections.singletonList(MatchStatus.OPEN));
        filters.setFormats(Collections.singletonList(MatchFormat.FIVE_V_FIVE));
        filters.setLatitude(48.8566);
        filters.setLongitude(2.3522);
        filters.setDistance(10.0);

        String authToken = "test-token";
        List<UUID> blockedUserIds = Collections.emptyList();

        Page<Match> matchPage = new PageImpl<>(Collections.singletonList(match));

        when(userBlockingService.getBlockedUserIds(anyString())).thenReturn(blockedUserIds);
        when(matchRepository.findAllByFilters(
                anyString(), anyList(), anyBoolean(), anyList(), anyBoolean(),
                anyList(), anyBoolean(), anyDouble(), anyDouble(), anyDouble(), any(Pageable.class)))
                .thenReturn(matchPage);
        when(playerRepository.existsByMatchIdAndPlayerId(matchId, creatorId)).thenReturn(true);
        when(playerRepository.countByMatchId(matchId)).thenReturn(1);
        when(savedMatchRepository.countByMatchId(matchId)).thenReturn(0);
        when(matchJoinRequestRepository.countByMatchIdAndRequestStatusNotIn(
                eq(matchId), anyList())).thenReturn(0);
        when(matchMapper.toMatchResponse(any(Match.class))).thenReturn(matchResponse);


        Page<MatchResponse> result = matchService.searchMatches(filters, pageable, creatorId, authToken);


        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(matchId, result.getContent().get(0).getId());


        verify(userBlockingService).getBlockedUserIds(anyString());
        verify(matchRepository).findAllByFilters(
                anyString(), anyList(), anyBoolean(), anyList(), anyBoolean(),
                anyList(), anyBoolean(), anyDouble(), anyDouble(), anyDouble(), any(Pageable.class));
        verify(playerRepository).existsByMatchIdAndPlayerId(matchId, creatorId);
        verify(playerRepository).countByMatchId(matchId);
        verify(savedMatchRepository).countByMatchId(matchId);
        verify(matchJoinRequestRepository).countByMatchIdAndRequestStatusNotIn(
                eq(matchId), anyList());
        verify(matchMapper).toMatchResponse(match);
    }

    @Test
    void searchMatches_NullFilter_ThrowsException() {

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                matchService.searchMatches(null, pageable, creatorId, "token"));

        assertEquals("Filter request cannot be null", exception.getMessage());
    }

    @Test
    void searchMatches_NullUserId_ThrowsException() {

        FilterMatchesRequest filters = new FilterMatchesRequest();


        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                matchService.searchMatches(filters, pageable, null, "token"));

        assertEquals("User ID cannot be null", exception.getMessage());
    }

    @Test
    void getUserUpcomingMatches_Success() {

        Page<Match> matchPage = new PageImpl<>(Collections.singletonList(match));

        when(matchRepository.findUserUpcomingMatches(
                eq(creatorId), any(ZonedDateTime.class), anyList(),
                eq(PlayerStatus.LEFT), eq(pageable)))
                .thenReturn(matchPage);
        when(playerRepository.countByMatchId(matchId)).thenReturn(1);
        when(savedMatchRepository.countByMatchId(matchId)).thenReturn(0);
        when(matchJoinRequestRepository.countByMatchIdAndRequestStatusNotIn(
                eq(matchId), anyList())).thenReturn(0);
        when(matchMapper.toMatchResponse(any(Match.class))).thenReturn(matchResponse);


        Page<MatchResponse> result = matchService.getUserUpcomingMatches(creatorId, pageable);


        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(matchId, result.getContent().get(0).getId());
        assertTrue(result.getContent().get(0).isJoined());


        verify(matchRepository).findUserUpcomingMatches(
                eq(creatorId), any(ZonedDateTime.class), anyList(),
                eq(PlayerStatus.LEFT), eq(pageable));
        verify(playerRepository).countByMatchId(matchId);
        verify(savedMatchRepository).countByMatchId(matchId);
        verify(matchJoinRequestRepository).countByMatchIdAndRequestStatusNotIn(
                eq(matchId), anyList());
        verify(matchMapper).toMatchResponse(match);
    }

    @Test
    void getMatchesByCreator_Success() {

        Page<Match> matchPage = new PageImpl<>(Collections.singletonList(match));

        when(matchRepository.findByCreatorId(creatorId, pageable)).thenReturn(matchPage);
        when(playerRepository.existsByMatchIdAndPlayerId(matchId, creatorId)).thenReturn(true);
        when(playerRepository.countByMatchId(matchId)).thenReturn(1);
        when(savedMatchRepository.countByMatchId(matchId)).thenReturn(0);
        when(matchJoinRequestRepository.countByMatchIdAndRequestStatusNotIn(
                eq(matchId), anyList())).thenReturn(0);
        when(matchMapper.toMatchResponse(any(Match.class))).thenReturn(matchResponse);


        Page<MatchResponse> result = matchService.getMatchesByCreator(creatorId, pageable);


        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(matchId, result.getContent().get(0).getId());
        assertTrue(result.getContent().get(0).isOwner());


        verify(matchRepository).findByCreatorId(creatorId, pageable);
        verify(playerRepository).existsByMatchIdAndPlayerId(matchId, creatorId);
        verify(playerRepository).countByMatchId(matchId);
        verify(savedMatchRepository).countByMatchId(matchId);
        verify(matchJoinRequestRepository).countByMatchIdAndRequestStatusNotIn(
                eq(matchId), anyList());
        verify(matchMapper).toMatchResponse(match);
    }

    @Test
    void deleteMatch_Success() {

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(feedbackRequestRepository.findByMatchId(matchId)).thenReturn(Optional.empty());


        matchService.deleteMatch(matchId);


        verify(matchRepository).findById(matchId);
        verify(playerRatingRepository).deleteByFeedbackFeedbackRequestIdIn(anyList());
        verify(playerFeedbackRepository).deleteByFeedbackRequestIdIn(anyList());
        verify(feedbackRequestRepository).deleteByMatchId(matchId);
        verify(scoreSubmissionRepository).deleteByMatchId(matchId);
        verify(matchResultRepository).deleteByMatchId(matchId);
        verify(matchJoinRequestRepository).deleteByMatchId(matchId);
        verify(playerRepository).deleteByMatchId(matchId);
        verify(matchTeamRepository).deleteByMatchId(matchId);
        verify(weatherService).deleteWeatherByMatchId(matchId);
        verify(savedMatchRepository).deleteByMatchId(matchId);
        verify(matchRepository).delete(match);
        verify(kafkaTemplate).send(anyString(), eq(matchId.toString()), any(MatchEvent.class));
    }

    @Test
    void deleteMatch_InvalidState() {

        match.setStatus(MatchStatus.IN_PROGRESS);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));


        InvalidMatchStateException exception = assertThrows(InvalidMatchStateException.class, () ->
                matchService.deleteMatch(matchId));

        assertEquals("Match cannot be deleted in status: " + MatchStatus.IN_PROGRESS, exception.getMessage());
    }

    @Test
    void updateMatchStatus_Success() {

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchRepository.save(any(Match.class))).thenReturn(match);
        when(matchMapper.toMatchResponse(any(Match.class))).thenReturn(matchResponse);


        MatchResponse result = matchService.updateMatchStatus(matchId, MatchStatus.IN_PROGRESS);


        assertNotNull(result);


        verify(matchRepository).findById(matchId);
        verify(matchRepository).save(match);
        verify(matchMapper).toMatchResponse(match);


        ArgumentCaptor<Match> matchCaptor = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository).save(matchCaptor.capture());
        assertEquals(MatchStatus.IN_PROGRESS, matchCaptor.getValue().getStatus());

        verify(kafkaTemplate).send(anyString(), eq(matchId.toString()), any(MatchEvent.class));
    }

    @Test
    void updateMatchStatus_InvalidTransition() {

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));


        InvalidMatchStateException exception = assertThrows(InvalidMatchStateException.class, () ->
                matchService.updateMatchStatus(matchId, MatchStatus.COMPLETED));

        assertEquals("Invalid status transition from OPEN to COMPLETED", exception.getMessage());
    }

    @Test
    void getMatchHistoryDetail_Success() {

        UUID userId = creatorId;
        match.setStatus(MatchStatus.FINISHED);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(playerRepository.findByMatchIdAndPlayerId(matchId, userId))
                .thenReturn(Optional.of(matchPlayer));
        when(matchTeamRepository.findByMatchId(matchId))
                .thenReturn(Arrays.asList(team1, team2));
        when(playerRepository.findByTeamId(team1.getId()))
                .thenReturn(Collections.singletonList(matchPlayer));
        when(playerRepository.findByTeamId(team2.getId()))
                .thenReturn(Collections.emptyList());
        when(matchResultRepository.findByMatchId(matchId))
                .thenReturn(Optional.empty());
        when(feedbackRequestRepository.findByMatchId(matchId))
                .thenReturn(Optional.empty());
        when(matchMapper.toMatchHistoryResponse(match))
                .thenReturn(new MatchHistoryResponse());


        MatchHistoryResponse result = matchService.getMatchHistoryDetail(matchId, userId);


        assertNotNull(result);


        verify(matchRepository).findById(matchId);
        verify(playerRepository).findByMatchIdAndPlayerId(matchId, userId);
        verify(matchTeamRepository).findByMatchId(matchId);
        verify(playerRepository).findByTeamId(team1.getId());
        verify(playerRepository).findByTeamId(team2.getId());
        verify(matchResultRepository).findByMatchId(matchId);
        verify(feedbackRequestRepository).findByMatchId(matchId);
        verify(matchMapper).toMatchHistoryResponse(match);
    }

    @Test
    void getMatchHistoryDetail_UserNotInMatch() {

        UUID userId = UUID.randomUUID();

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(playerRepository.findByMatchIdAndPlayerId(matchId, userId))
                .thenReturn(Optional.empty());


        MatchException exception = assertThrows(MatchException.class, () ->
                matchService.getMatchHistoryDetail(matchId, userId));

        assertEquals("User did not participate in this match: " + matchId, exception.getMessage());
        assertEquals("USER_NOT_IN_MATCH", exception.getErrorCode());
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void getMatchHistory_Success() {

        UUID userId = creatorId;
        match.setStatus(MatchStatus.FINISHED);

        Page<MatchPlayer> playerPage = new PageImpl<>(Collections.singletonList(matchPlayer));

        when(playerRepository.findByPlayerIdAndMatchStatusOrderByMatchStartDateDesc(
                eq(userId), eq(MatchStatus.FINISHED), eq(pageable)))
                .thenReturn(playerPage);
        when(matchTeamRepository.findByMatchId(matchId))
                .thenReturn(Arrays.asList(team1, team2));
        when(playerRepository.findByTeamId(team1.getId()))
                .thenReturn(Collections.singletonList(matchPlayer));
        when(playerRepository.findByTeamId(team2.getId()))
                .thenReturn(Collections.emptyList());
        when(matchResultRepository.findByMatchId(matchId))
                .thenReturn(Optional.empty());
        when(feedbackRequestRepository.findByMatchId(matchId))
                .thenReturn(Optional.empty());
        when(matchMapper.toMatchHistoryResponse(match))
                .thenReturn(new MatchHistoryResponse());


        Page<MatchHistoryResponse> result = matchService.getMatchHistory(userId, pageable);


        assertNotNull(result);
        assertEquals(1, result.getTotalElements());


        verify(playerRepository).findByPlayerIdAndMatchStatusOrderByMatchStartDateDesc(
                eq(userId), eq(MatchStatus.FINISHED), eq(pageable));
        verify(matchTeamRepository).findByMatchId(matchId);
        verify(playerRepository).findByTeamId(team1.getId());
        verify(playerRepository).findByTeamId(team2.getId());
        verify(matchResultRepository).findByMatchId(matchId);
        verify(feedbackRequestRepository).findByMatchId(matchId);
        verify(matchMapper).toMatchHistoryResponse(match);
    }
}