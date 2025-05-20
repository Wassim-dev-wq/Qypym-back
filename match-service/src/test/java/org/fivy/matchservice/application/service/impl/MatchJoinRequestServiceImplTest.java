package org.fivy.matchservice.application.service.impl;

import org.fivy.matchservice.api.dto.response.MatchJoinRequestResponse;
import org.fivy.matchservice.domain.entity.Match;
import org.fivy.matchservice.domain.entity.MatchJoinRequest;
import org.fivy.matchservice.domain.entity.MatchPlayer;
import org.fivy.matchservice.domain.entity.MatchTeam;
import org.fivy.matchservice.domain.enums.JoinRequestStatus;
import org.fivy.matchservice.domain.enums.MatchStatus;
import org.fivy.matchservice.domain.enums.PlayerRole;
import org.fivy.matchservice.domain.event.MatchEventType;
import org.fivy.matchservice.domain.repository.MatchJoinRequestRepository;
import org.fivy.matchservice.domain.repository.MatchPlayerRepository;
import org.fivy.matchservice.domain.repository.MatchRepository;
import org.fivy.matchservice.domain.repository.MatchTeamRepository;
import org.fivy.matchservice.shared.exception.MatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MatchJoinRequestServiceImplTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchJoinRequestRepository matchJoinRequestRepository;

    @Mock
    private MatchTeamRepository matchTeamRepository;

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private MatchJoinRequestServiceImpl matchJoinRequestService;

    private UUID matchId;
    private UUID teamId;
    private UUID userId;
    private UUID creatorId;
    private UUID requestId;
    private Match match;
    private MatchTeam team;
    private MatchJoinRequest joinRequest;
    private MatchJoinRequestResponse joinRequestResponse;

    @BeforeEach
    void setUp() {
        matchId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        userId = UUID.randomUUID();
        creatorId = UUID.randomUUID();
        requestId = UUID.randomUUID();


        match = new Match();
        match.setId(matchId);
        match.setCreatorId(creatorId);
        match.setStatus(MatchStatus.OPEN);


        team = new MatchTeam();
        team.setId(teamId);
        team.setMatch(match);
        team.setTeamNumber(1);
        team.setName("Team 1");


        joinRequest = MatchJoinRequest.builder()
                .id(requestId)
                .match(match)
                .userId(userId)
                .preferredTeam(team)
                .message("I'd like to join")
                .position("milieu")
                .experience("5 years")
                .personalNote("I'm very reliable")
                .isAvailable(true)
                .requestStatus(JoinRequestStatus.PENDING)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();


        joinRequestResponse = MatchJoinRequestResponse.builder()
                .id(requestId)
                .matchId(matchId)
                .userId(userId)
                .preferredTeamId(teamId)
                .message("I'd like to join")
                .position("milieu")
                .experience("5 years")
                .personalNote("I'm very reliable")
                .isAvailable(true)
                .requestStatus(JoinRequestStatus.PENDING)
                .createdAt(joinRequest.getCreatedAt())
                .updatedAt(joinRequest.getUpdatedAt())
                .build();
    }

    @Test
    void requestToJoin_Success() {

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchTeamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(matchJoinRequestRepository.findByMatchIdAndUserId(matchId, userId)).thenReturn(Optional.empty());
        when(matchJoinRequestRepository.save(any(MatchJoinRequest.class))).thenReturn(joinRequest);
        doNothing().when(notificationService).sendJoinRequestNotification(any(Match.class), anyString());


        MatchJoinRequestResponse result = matchJoinRequestService.requestToJoin(
                matchId, userId, teamId, "I'd like to join", "milieu",
                "5 years", "I'm very reliable", true);


        assertNotNull(result);
        assertEquals(requestId, result.getId());
        assertEquals(matchId, result.getMatchId());
        assertEquals(userId, result.getUserId());
        assertEquals(teamId, result.getPreferredTeamId());
        assertEquals(JoinRequestStatus.PENDING, result.getRequestStatus());


        verify(matchRepository).findById(matchId);
        verify(matchTeamRepository).findById(teamId);
        verify(matchJoinRequestRepository).findByMatchIdAndUserId(matchId, userId);
        verify(matchJoinRequestRepository).save(any(MatchJoinRequest.class));
        verify(notificationService).sendJoinRequestNotification(eq(match), eq("Un milieu"));
    }

    @Test
    void requestToJoin_MatchNotFound() {

        when(matchRepository.findById(matchId)).thenReturn(Optional.empty());


        MatchException exception = assertThrows(MatchException.class, () ->
                matchJoinRequestService.requestToJoin(
                        matchId, userId, teamId, "message", "position",
                        "experience", "note", true));

        assertEquals("Match not found: " + matchId, exception.getMessage());
        assertEquals("MATCH_NOT_FOUND", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void requestToJoin_MatchNotOpen() {

        match.setStatus(MatchStatus.FINISHED);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));


        MatchException exception = assertThrows(MatchException.class, () ->
                matchJoinRequestService.requestToJoin(
                        matchId, userId, teamId, "message", "position",
                        "experience", "note", true));

        assertEquals("Match is not open for requests", exception.getMessage());
        assertEquals("MATCH_NOT_OPEN", exception.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void requestToJoin_CreatorCannotJoin() {

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));


        MatchException exception = assertThrows(MatchException.class, () ->
                matchJoinRequestService.requestToJoin(
                        matchId, creatorId, teamId, "message", "position",
                        "experience", "note", true));

        assertEquals("Creator cannot request to join his own match", exception.getMessage());
        assertEquals("CREATOR_CANNOT_JOIN", exception.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void requestToJoin_TeamNotFound() {

        UUID nonExistentTeamId = UUID.randomUUID();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchTeamRepository.findById(nonExistentTeamId)).thenReturn(Optional.empty());


        MatchException exception = assertThrows(MatchException.class, () ->
                matchJoinRequestService.requestToJoin(
                        matchId, userId, nonExistentTeamId, "message", "position",
                        "experience", "note", true));

        assertEquals("Preferred team not found: " + nonExistentTeamId, exception.getMessage());
        assertEquals("TEAM_NOT_FOUND", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void requestToJoin_TeamNotInMatch() {

        MatchTeam otherTeam = new MatchTeam();
        Match otherMatch = new Match();
        otherMatch.setId(UUID.randomUUID());
        otherTeam.setId(UUID.randomUUID());
        otherTeam.setMatch(otherMatch);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchTeamRepository.findById(otherTeam.getId())).thenReturn(Optional.of(otherTeam));


        MatchException exception = assertThrows(MatchException.class, () ->
                matchJoinRequestService.requestToJoin(
                        matchId, userId, otherTeam.getId(), "message", "position",
                        "experience", "note", true));

        assertEquals("Preferred team does not belong to this match", exception.getMessage());
        assertEquals("INVALID_TEAM_MATCH", exception.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void requestToJoin_RequestAlreadyExists() {

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchTeamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(matchJoinRequestRepository.findByMatchIdAndUserId(matchId, userId)).thenReturn(Optional.of(joinRequest));


        MatchException exception = assertThrows(MatchException.class, () ->
                matchJoinRequestService.requestToJoin(
                        matchId, userId, teamId, "message", "position",
                        "experience", "note", true));

        assertEquals("User already joined/requested this match", exception.getMessage());
        assertEquals("DUPLICATE_REQUEST", exception.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void requestToJoin_ReactivateCanceledRequest() {

        joinRequest.setRequestStatus(JoinRequestStatus.CANCELED);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchTeamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(matchJoinRequestRepository.findByMatchIdAndUserId(matchId, userId)).thenReturn(Optional.of(joinRequest));
        when(matchJoinRequestRepository.save(any(MatchJoinRequest.class))).thenReturn(joinRequest);
        doNothing().when(notificationService).sendJoinRequestNotification(any(Match.class), anyString());


        MatchJoinRequestResponse result = matchJoinRequestService.requestToJoin(
                matchId, userId, teamId, "New message", "milieu",
                "6 years", "I'm still reliable", true);


        assertNotNull(result);


        ArgumentCaptor<MatchJoinRequest> requestCaptor = ArgumentCaptor.forClass(MatchJoinRequest.class);
        verify(matchJoinRequestRepository).save(requestCaptor.capture());

        MatchJoinRequest savedRequest = requestCaptor.getValue();
        assertEquals(JoinRequestStatus.PENDING, savedRequest.getRequestStatus());
        assertEquals("New message", savedRequest.getMessage());
        assertEquals("6 years", savedRequest.getExperience());
        assertEquals("I'm still reliable", savedRequest.getPersonalNote());
    }

    @Test
    void getJoinRequest_Exists() {

        when(matchJoinRequestRepository.findByMatchIdAndUserId(matchId, userId)).thenReturn(Optional.of(joinRequest));


        MatchJoinRequestResponse result = matchJoinRequestService.getJoinRequest(matchId, userId);


        assertNotNull(result);
        assertEquals(requestId, result.getId());
        assertEquals(matchId, result.getMatchId());
        assertEquals(userId, result.getUserId());
        assertEquals(JoinRequestStatus.PENDING, result.getRequestStatus());
    }

    @Test
    void getJoinRequest_NotExists() {

        when(matchJoinRequestRepository.findByMatchIdAndUserId(matchId, userId)).thenReturn(Optional.empty());


        MatchJoinRequestResponse result = matchJoinRequestService.getJoinRequest(matchId, userId);


        assertNotNull(result);
        assertEquals(matchId, result.getMatchId());
        assertEquals(userId, result.getUserId());
        assertEquals(JoinRequestStatus.NOT_REQUESTED, result.getRequestStatus());
    }

    @Test
    void acceptJoinRequest_Success() {
        when(matchJoinRequestRepository.findById(requestId)).thenReturn(Optional.of(joinRequest));
        when(matchTeamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(matchJoinRequestRepository.save(any(MatchJoinRequest.class))).thenReturn(joinRequest);
        when(matchPlayerRepository.save(any(MatchPlayer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(notificationService).sendJoinRequestResponseNotification(any(), any(), anyBoolean());
        doNothing().when(notificationService).sendMatchEventNotifications(any(), any());

        MatchJoinRequestResponse result = matchJoinRequestService.acceptJoinRequest(requestId, creatorId, teamId);
        assertNotNull(result);
        assertEquals(requestId, result.getId());
        ArgumentCaptor<MatchJoinRequest> requestCaptor = ArgumentCaptor.forClass(MatchJoinRequest.class);
        verify(matchJoinRequestRepository).save(requestCaptor.capture());
        MatchJoinRequest savedRequest = requestCaptor.getValue();
        assertEquals(JoinRequestStatus.ACCEPTED, savedRequest.getRequestStatus());


        ArgumentCaptor<MatchPlayer> playerCaptor = ArgumentCaptor.forClass(MatchPlayer.class);
        verify(matchPlayerRepository).save(playerCaptor.capture());

        MatchPlayer createdPlayer = playerCaptor.getValue();
        assertEquals(userId, createdPlayer.getPlayerId());
        assertEquals(match, createdPlayer.getMatch());
        assertEquals(team, createdPlayer.getTeam());
        assertEquals(PlayerRole.MIDFIELDER, createdPlayer.getRole());


        verify(notificationService).sendJoinRequestResponseNotification(eq(match), eq(userId), eq(true));
        verify(notificationService).sendMatchEventNotifications(eq(match), eq(MatchEventType.PLAYER_JOINED));
    }

    @Test
    void acceptJoinRequest_RequestNotFound() {

        when(matchJoinRequestRepository.findById(requestId)).thenReturn(Optional.empty());


        MatchException exception = assertThrows(MatchException.class, () ->
                matchJoinRequestService.acceptJoinRequest(requestId, creatorId, teamId));

        assertEquals("Join request not found: " + requestId, exception.getMessage());
        assertEquals("JOIN_REQUEST_NOT_FOUND", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void acceptJoinRequest_NotMatchOwner() {

        UUID otherUserId = UUID.randomUUID();
        when(matchJoinRequestRepository.findById(requestId)).thenReturn(Optional.of(joinRequest));


        MatchException exception = assertThrows(MatchException.class, () ->
                matchJoinRequestService.acceptJoinRequest(requestId, otherUserId, teamId));

        assertEquals("User is not the match owner", exception.getMessage());
        assertEquals("NOT_MATCH_OWNER", exception.getErrorCode());
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void acceptJoinRequest_InvalidRequestStatus() {

        joinRequest.setRequestStatus(JoinRequestStatus.ACCEPTED);
        when(matchJoinRequestRepository.findById(requestId)).thenReturn(Optional.of(joinRequest));


        MatchException exception = assertThrows(MatchException.class, () ->
                matchJoinRequestService.acceptJoinRequest(requestId, creatorId, teamId));

        assertEquals("Join request is not in PENDING status", exception.getMessage());
        assertEquals("INVALID_REQUEST_STATUS", exception.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void acceptJoinRequest_AssignedTeamNotFound() {

        UUID nonExistentTeamId = UUID.randomUUID();
        when(matchJoinRequestRepository.findById(requestId)).thenReturn(Optional.of(joinRequest));
        when(matchTeamRepository.findById(nonExistentTeamId)).thenReturn(Optional.empty());


        MatchException exception = assertThrows(MatchException.class, () ->
                matchJoinRequestService.acceptJoinRequest(requestId, creatorId, nonExistentTeamId));

        assertEquals("Assigned team not found: " + nonExistentTeamId, exception.getMessage());
        assertEquals("TEAM_NOT_FOUND", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void acceptJoinRequest_AssignedTeamNotInMatch() {

        MatchTeam otherTeam = new MatchTeam();
        Match otherMatch = new Match();
        otherMatch.setId(UUID.randomUUID());
        otherTeam.setId(UUID.randomUUID());
        otherTeam.setMatch(otherMatch);

        when(matchJoinRequestRepository.findById(requestId)).thenReturn(Optional.of(joinRequest));
        when(matchTeamRepository.findById(otherTeam.getId())).thenReturn(Optional.of(otherTeam));


        MatchException exception = assertThrows(MatchException.class, () ->
                matchJoinRequestService.acceptJoinRequest(requestId, creatorId, otherTeam.getId()));

        assertEquals("Assigned team does not belong to this match", exception.getMessage());
        assertEquals("INVALID_TEAM_MATCH", exception.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void rejectJoinRequest_Success() {

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchJoinRequestRepository.findById(requestId)).thenReturn(Optional.of(joinRequest));
        when(matchJoinRequestRepository.save(any(MatchJoinRequest.class))).thenReturn(joinRequest);
        doNothing().when(notificationService).sendJoinRequestResponseNotification(any(), any(), anyBoolean());


        MatchJoinRequestResponse result = matchJoinRequestService.rejectJoinRequest(matchId, requestId, creatorId);


        assertNotNull(result);


        ArgumentCaptor<MatchJoinRequest> requestCaptor = ArgumentCaptor.forClass(MatchJoinRequest.class);
        verify(matchJoinRequestRepository).save(requestCaptor.capture());

        MatchJoinRequest savedRequest = requestCaptor.getValue();
        assertEquals(JoinRequestStatus.DECLINED, savedRequest.getRequestStatus());


        verify(notificationService).sendJoinRequestResponseNotification(eq(match), eq(userId), eq(false));
    }

    @Test
    void rejectJoinRequest_MatchNotFound() {

        when(matchRepository.findById(matchId)).thenReturn(Optional.empty());


        MatchException exception = assertThrows(MatchException.class, () ->
                matchJoinRequestService.rejectJoinRequest(matchId, requestId, creatorId));

        assertEquals("Match not found: " + matchId, exception.getMessage());
        assertEquals("MATCH_NOT_FOUND", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void rejectJoinRequest_NotMatchOwner() {

        UUID otherUserId = UUID.randomUUID();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));


        MatchException exception = assertThrows(MatchException.class, () ->
                matchJoinRequestService.rejectJoinRequest(matchId, requestId, otherUserId));

        assertEquals("User is not the match owner", exception.getMessage());
        assertEquals("NOT_MATCH_OWNER", exception.getErrorCode());
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void cancelJoinRequest_Success() {

        when(matchJoinRequestRepository.findById(requestId)).thenReturn(Optional.of(joinRequest));
        when(matchJoinRequestRepository.save(any(MatchJoinRequest.class))).thenReturn(joinRequest);


        MatchJoinRequestResponse result = matchJoinRequestService.cancelJoinRequest(requestId, userId);


        assertNotNull(result);


        ArgumentCaptor<MatchJoinRequest> requestCaptor = ArgumentCaptor.forClass(MatchJoinRequest.class);
        verify(matchJoinRequestRepository).save(requestCaptor.capture());

        MatchJoinRequest savedRequest = requestCaptor.getValue();
        assertEquals(JoinRequestStatus.CANCELED, savedRequest.getRequestStatus());
    }

    @Test
    void cancelJoinRequest_RequestNotFound() {

        when(matchJoinRequestRepository.findById(requestId)).thenReturn(Optional.empty());


        MatchException exception = assertThrows(MatchException.class, () ->
                matchJoinRequestService.cancelJoinRequest(requestId, userId));

        assertEquals("Join request not found: " + requestId, exception.getMessage());
        assertEquals("JOIN_REQUEST_NOT_FOUND", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void cancelJoinRequest_NotRequestOwner() {

        UUID otherUserId = UUID.randomUUID();
        when(matchJoinRequestRepository.findById(requestId)).thenReturn(Optional.of(joinRequest));


        MatchException exception = assertThrows(MatchException.class, () ->
                matchJoinRequestService.cancelJoinRequest(requestId, otherUserId));

        assertEquals("User is not the join request owner", exception.getMessage());
        assertEquals("NOT_REQUEST_OWNER", exception.getErrorCode());
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void cancelJoinRequest_InvalidRequestStatus() {

        joinRequest.setRequestStatus(JoinRequestStatus.ACCEPTED);
        when(matchJoinRequestRepository.findById(requestId)).thenReturn(Optional.of(joinRequest));


        MatchException exception = assertThrows(MatchException.class, () ->
                matchJoinRequestService.cancelJoinRequest(requestId, userId));

        assertEquals("Join request is not in PENDING status", exception.getMessage());
        assertEquals("INVALID_REQUEST_STATUS", exception.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void getJoinRequests_Success() {

        List<MatchJoinRequest> requests = List.of(joinRequest);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchJoinRequestRepository.findAllByMatchId(matchId)).thenReturn(requests);


        List<MatchJoinRequestResponse> result = matchJoinRequestService.getJoinRequests(matchId, creatorId);


        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(requestId, result.get(0).getId());
        assertEquals(matchId, result.get(0).getMatchId());
        assertEquals(userId, result.get(0).getUserId());
    }

    @Test
    void getUserJoinRequests_Success() {

        List<MatchJoinRequest> requests = List.of(joinRequest);
        when(matchJoinRequestRepository.findAllByUserId(userId)).thenReturn(requests);


        List<MatchJoinRequestResponse> result = matchJoinRequestService.getUserJoinRequests(userId);


        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(requestId, result.get(0).getId());
        assertEquals(matchId, result.get(0).getMatchId());
        assertEquals(userId, result.get(0).getUserId());
    }
}