package org.fivy.matchservice.application.service.impl;

import org.fivy.matchservice.api.dto.response.MatchPlayerResponse;
import org.fivy.matchservice.domain.entity.Match;
import org.fivy.matchservice.domain.entity.MatchJoinRequest;
import org.fivy.matchservice.domain.entity.MatchPlayer;
import org.fivy.matchservice.domain.entity.MatchTeam;
import org.fivy.matchservice.domain.enums.JoinRequestStatus;
import org.fivy.matchservice.domain.enums.MatchStatus;
import org.fivy.matchservice.domain.enums.PlayerRole;
import org.fivy.matchservice.domain.enums.PlayerStatus;
import org.fivy.matchservice.domain.event.MatchEvent;
import org.fivy.matchservice.domain.event.MatchEventType;
import org.fivy.matchservice.domain.repository.MatchJoinRequestRepository;
import org.fivy.matchservice.domain.repository.MatchPlayerRepository;
import org.fivy.matchservice.domain.repository.MatchRepository;
import org.fivy.matchservice.shared.exception.MatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MatchPlayerServiceImplTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @Mock
    private MatchJoinRequestRepository matchJoinRequestRepository;

    @Mock
    private KafkaTemplate<String, MatchEvent> kafkaTemplate;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private MatchPlayerServiceImpl matchPlayerService;

    private UUID matchId;
    private UUID playerId;
    private UUID creatorId;
    private UUID teamId;
    private Match match;
    private MatchPlayer matchPlayer;
    private MatchJoinRequest joinRequest;
    private MatchTeam team;

    @BeforeEach
    void setUp() {
        matchId = UUID.randomUUID();
        playerId = UUID.randomUUID();
        creatorId = UUID.randomUUID();
        teamId = UUID.randomUUID();


        match = new Match();
        match.setId(matchId);
        match.setCreatorId(creatorId);
        match.setStatus(MatchStatus.OPEN);


        team = new MatchTeam();
        team.setId(teamId);
        team.setMatch(match);
        team.setTeamNumber(1);
        team.setName("Team 1");


        matchPlayer = new MatchPlayer();
        matchPlayer.setId(UUID.randomUUID());
        matchPlayer.setMatch(match);
        matchPlayer.setPlayerId(playerId);
        matchPlayer.setStatus(PlayerStatus.JOINED);
        matchPlayer.setRole(PlayerRole.MIDFIELDER);
        matchPlayer.setTeam(team);
        matchPlayer.setJoinedAt(LocalDateTime.now());


        joinRequest = new MatchJoinRequest();
        joinRequest.setId(UUID.randomUUID());
        joinRequest.setMatch(match);
        joinRequest.setUserId(playerId);
        joinRequest.setRequestStatus(JoinRequestStatus.ACCEPTED);


        lenient().when(kafkaTemplate.send(anyString(), anyString(), any(MatchEvent.class)))
                .thenReturn(mock(CompletableFuture.class));
    }

    @Test
    void updatePlayerStatus_Success() {

        PlayerStatus newStatus = PlayerStatus.LEFT;

        when(matchPlayerRepository.findByPlayerId(playerId)).thenReturn(Optional.of(matchPlayer));
        when(matchPlayerRepository.save(any(MatchPlayer.class))).thenReturn(matchPlayer);


        matchPlayerService.updatePlayerStatus(playerId, playerId, newStatus);


        ArgumentCaptor<MatchPlayer> playerCaptor = ArgumentCaptor.forClass(MatchPlayer.class);
        verify(matchPlayerRepository).save(playerCaptor.capture());

        assertEquals(newStatus, playerCaptor.getValue().getStatus());


        verify(kafkaTemplate).send(anyString(), eq(matchId.toString()), any(MatchEvent.class));
    }

    @Test
    void updatePlayerStatus_PlayerNotFound() {

        when(matchPlayerRepository.findByPlayerId(playerId)).thenReturn(Optional.empty());


        MatchException exception = assertThrows(MatchException.class, () ->
                matchPlayerService.updatePlayerStatus(playerId, playerId, PlayerStatus.LEFT));

        assertEquals("Player not found with ID: " + playerId, exception.getMessage());
        assertEquals("PLAYER_NOT_FOUND", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void updatePlayerStatus_InvalidMatchState() {

        match.setStatus(MatchStatus.FINISHED);

        when(matchPlayerRepository.findByPlayerId(playerId)).thenReturn(Optional.of(matchPlayer));


        MatchException exception = assertThrows(MatchException.class, () ->
                matchPlayerService.updatePlayerStatus(playerId, playerId, PlayerStatus.LEFT));

        assertEquals("Cannot update player status when match is in FINISHED state", exception.getMessage());
        assertEquals("INVALID_MATCH_STATE", exception.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void updatePlayerStatus_NotAuthorized() {

        UUID otherUserId = UUID.randomUUID();

        when(matchPlayerRepository.findByPlayerId(playerId)).thenReturn(Optional.of(matchPlayer));


        MatchException exception = assertThrows(MatchException.class, () ->
                matchPlayerService.updatePlayerStatus(playerId, otherUserId, PlayerStatus.LEFT));

        assertEquals("Not authorized to update this player's status", exception.getMessage());
        assertEquals("NOT_AUTHORIZED", exception.getErrorCode());
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void updatePlayerStatus_AsCreator_Success() {

        PlayerStatus newStatus = PlayerStatus.LEFT;

        when(matchPlayerRepository.findByPlayerId(playerId)).thenReturn(Optional.of(matchPlayer));
        when(matchPlayerRepository.save(any(MatchPlayer.class))).thenReturn(matchPlayer);


        matchPlayerService.updatePlayerStatus(playerId, creatorId, newStatus);


        ArgumentCaptor<MatchPlayer> playerCaptor = ArgumentCaptor.forClass(MatchPlayer.class);
        verify(matchPlayerRepository).save(playerCaptor.capture());

        assertEquals(newStatus, playerCaptor.getValue().getStatus());


        verify(kafkaTemplate).send(anyString(), eq(matchId.toString()), any(MatchEvent.class));
    }

    @Test
    void getPlayerStatus_AsSamePlayer_Success() {

        when(matchPlayerRepository.findByPlayerId(playerId)).thenReturn(Optional.of(matchPlayer));


        MatchPlayerResponse response = matchPlayerService.getPlayerStatus(playerId, playerId);


        assertNotNull(response);
        assertEquals(playerId, response.getPlayerId());
        assertEquals(matchId, response.getMatchId());
        assertEquals(PlayerStatus.JOINED, response.getStatus());
        assertEquals(PlayerRole.MIDFIELDER, response.getRole());
        assertEquals(teamId, response.getTeamId());
        assertEquals("Team 1", response.getTeamName());
        assertEquals(1, response.getTeamNumber());
    }

    @Test
    void getPlayerStatus_AsCreator_Success() {

        when(matchPlayerRepository.findByPlayerId(playerId)).thenReturn(Optional.of(matchPlayer));


        MatchPlayerResponse response = matchPlayerService.getPlayerStatus(playerId, creatorId);


        assertNotNull(response);
        assertEquals(playerId, response.getPlayerId());
    }

    @Test
    void getPlayerStatus_AsTeammate_Success() {

        UUID teammateId = UUID.randomUUID();

        when(matchPlayerRepository.findByPlayerId(playerId)).thenReturn(Optional.of(matchPlayer));
        when(matchPlayerRepository.existsByMatchIdAndPlayerId(matchId, teammateId)).thenReturn(true);


        MatchPlayerResponse response = matchPlayerService.getPlayerStatus(playerId, teammateId);


        assertNotNull(response);
        assertEquals(playerId, response.getPlayerId());
    }

    @Test
    void getPlayerStatus_NotAuthorized() {

        UUID otherUserId = UUID.randomUUID();

        when(matchPlayerRepository.findByPlayerId(playerId)).thenReturn(Optional.of(matchPlayer));
        when(matchPlayerRepository.existsByMatchIdAndPlayerId(matchId, otherUserId)).thenReturn(false);


        MatchException exception = assertThrows(MatchException.class, () ->
                matchPlayerService.getPlayerStatus(playerId, otherUserId));

        assertEquals("Not authorized to view this player's status", exception.getMessage());
        assertEquals("NOT_AUTHORIZED", exception.getErrorCode());
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void getPlayerStatus_PlayerNotFound() {

        when(matchPlayerRepository.findByPlayerId(playerId)).thenReturn(Optional.empty());


        MatchException exception = assertThrows(MatchException.class, () ->
                matchPlayerService.getPlayerStatus(playerId, playerId));

        assertEquals("Player not found with ID: " + playerId, exception.getMessage());
        assertEquals("PLAYER_NOT_FOUND", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void getPlayersStatus_ReturnsNull() {

        assertNull(matchPlayerService.getPlayersStatus(playerId));
    }

    @Test
    void leaveMatch_Success() {

        UUID requestId = UUID.randomUUID();

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchIdAndPlayerId(matchId, playerId)).thenReturn(Optional.of(matchPlayer));
        when(matchJoinRequestRepository.findByMatchIdAndUserId(matchId, playerId)).thenReturn(Optional.of(joinRequest));
        when(matchJoinRequestRepository.save(any(MatchJoinRequest.class))).thenReturn(joinRequest);
        when(matchPlayerRepository.save(any(MatchPlayer.class))).thenReturn(matchPlayer);


        matchPlayerService.leaveMatch(requestId, matchId, playerId);


        ArgumentCaptor<MatchJoinRequest> requestCaptor = ArgumentCaptor.forClass(MatchJoinRequest.class);
        verify(matchJoinRequestRepository).save(requestCaptor.capture());

        assertEquals(JoinRequestStatus.LEFT, requestCaptor.getValue().getRequestStatus());

        ArgumentCaptor<MatchPlayer> playerCaptor = ArgumentCaptor.forClass(MatchPlayer.class);
        verify(matchPlayerRepository).save(playerCaptor.capture());

        assertEquals(PlayerStatus.LEFT, playerCaptor.getValue().getStatus());
    }

    @Test
    void leaveMatch_MatchNotFound() {

        UUID requestId = UUID.randomUUID();

        when(matchRepository.findById(matchId)).thenReturn(Optional.empty());


        MatchException exception = assertThrows(MatchException.class, () ->
                matchPlayerService.leaveMatch(requestId, matchId, playerId));

        assertEquals("Match not found with ID: " + matchId, exception.getMessage());
        assertEquals("MATCH_NOT_FOUND", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void leaveMatch_InvalidMatchState() {

        UUID requestId = UUID.randomUUID();
        match.setStatus(MatchStatus.FINISHED);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));


        MatchException exception = assertThrows(MatchException.class, () ->
                matchPlayerService.leaveMatch(requestId, matchId, playerId));

        assertEquals("Cannot leave match when it is in FINISHED state", exception.getMessage());
        assertEquals("INVALID_MATCH_STATE", exception.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void leaveMatch_PlayerNotInMatch() {

        UUID requestId = UUID.randomUUID();

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchIdAndPlayerId(matchId, playerId)).thenReturn(Optional.empty());


        MatchException exception = assertThrows(MatchException.class, () ->
                matchPlayerService.leaveMatch(requestId, matchId, playerId));

        assertEquals("Player not found in this match", exception.getMessage());
        assertEquals("PLAYER_NOT_IN_MATCH", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void leaveMatch_JoinRequestNotFound() {

        UUID requestId = UUID.randomUUID();

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchIdAndPlayerId(matchId, playerId)).thenReturn(Optional.of(matchPlayer));
        when(matchJoinRequestRepository.findByMatchIdAndUserId(matchId, playerId)).thenReturn(Optional.empty());


        MatchException exception = assertThrows(MatchException.class, () ->
                matchPlayerService.leaveMatch(requestId, matchId, playerId));

        assertEquals("Join request not found for user: " + playerId, exception.getMessage());
        assertEquals("JOIN_REQUEST_NOT_FOUND", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void leaveMatch_PlayerAlreadyLeft() {

        UUID requestId = UUID.randomUUID();
        matchPlayer.setStatus(PlayerStatus.LEFT);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchIdAndPlayerId(matchId, playerId)).thenReturn(Optional.of(matchPlayer));


        matchPlayerService.leaveMatch(requestId, matchId, playerId);


        verify(matchJoinRequestRepository, never()).findByMatchIdAndUserId(any(), any());
        verify(matchJoinRequestRepository, never()).save(any());
        verify(matchPlayerRepository, never()).save(any());
    }

    @Test
    void leaveMatch_AsCreator_Success() {

        UUID requestId = UUID.randomUUID();
        matchPlayer.setPlayerId(creatorId);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchIdAndPlayerId(matchId, creatorId)).thenReturn(Optional.of(matchPlayer));
        when(matchJoinRequestRepository.findByMatchIdAndUserId(matchId, creatorId)).thenReturn(Optional.of(joinRequest));
        when(matchJoinRequestRepository.save(any(MatchJoinRequest.class))).thenReturn(joinRequest);
        when(matchPlayerRepository.save(any(MatchPlayer.class))).thenReturn(matchPlayer);


        matchPlayerService.leaveMatch(requestId, matchId, creatorId);


        ArgumentCaptor<MatchJoinRequest> requestCaptor = ArgumentCaptor.forClass(MatchJoinRequest.class);
        verify(matchJoinRequestRepository).save(requestCaptor.capture());

        assertEquals(JoinRequestStatus.LEFT, requestCaptor.getValue().getRequestStatus());

        ArgumentCaptor<MatchPlayer> playerCaptor = ArgumentCaptor.forClass(MatchPlayer.class);
        verify(matchPlayerRepository).save(playerCaptor.capture());

        assertEquals(PlayerStatus.LEFT, playerCaptor.getValue().getStatus());
    }
}