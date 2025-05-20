package org.fivy.matchservice.application.service.impl;

import org.fivy.matchservice.domain.entity.Match;
import org.fivy.matchservice.domain.entity.MatchResult;
import org.fivy.matchservice.domain.entity.MatchScoreSubmission;
import org.fivy.matchservice.domain.entity.MatchTeam;
import org.fivy.matchservice.domain.enums.MatchResultStatus;
import org.fivy.matchservice.domain.enums.ScoreSubmissionStatus;
import org.fivy.matchservice.domain.repository.MatchPlayerRepository;
import org.fivy.matchservice.domain.repository.MatchRepository;
import org.fivy.matchservice.domain.repository.MatchResultRepository;
import org.fivy.matchservice.domain.repository.MatchScoreSubmissionRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MatchResultServiceImplTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchTeamRepository matchTeamRepository;

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @Mock
    private MatchResultRepository matchResultRepository;

    @Mock
    private MatchScoreSubmissionRepository scoreSubmissionRepository;

    @InjectMocks
    private MatchResultServiceImpl matchResultService;

    private UUID matchId;
    private UUID userId;
    private UUID team1Id;
    private UUID team2Id;
    private UUID resultId;
    private Match match;
    private MatchTeam team1;
    private MatchTeam team2;
    private MatchResult matchResult;
    private MatchScoreSubmission scoreSubmission;

    @BeforeEach
    void setUp() {
        matchId = UUID.randomUUID();
        userId = UUID.randomUUID();
        team1Id = UUID.randomUUID();
        team2Id = UUID.randomUUID();
        resultId = UUID.randomUUID();


        match = new Match();
        match.setId(matchId);


        team1 = new MatchTeam();
        team1.setId(team1Id);
        team1.setMatch(match);
        team1.setTeamNumber(1);
        team1.setName("Team 1");

        team2 = new MatchTeam();
        team2.setId(team2Id);
        team2.setMatch(match);
        team2.setTeamNumber(2);
        team2.setName("Team 2");


        matchResult = MatchResult.builder()
                .id(resultId)
                .match(match)
                .status(MatchResultStatus.PENDING)
                .createdAt(ZonedDateTime.now())
                .build();


        scoreSubmission = MatchScoreSubmission.builder()
                .match(match)
                .submitterId(userId)
                .team1(team1)
                .team2(team2)
                .team1Score(3)
                .team2Score(1)
                .status(ScoreSubmissionStatus.PENDING)
                .createdAt(ZonedDateTime.now())
                .build();


        ReflectionTestUtils.setField(matchResultService, "consensusThresholdPercent", 50);
        ReflectionTestUtils.setField(matchResultService, "minSubmissionsRequired", 2);
    }

    @Test
    void initializeMatchResult_ResultDoesNotExist_CreatesNewResult() {

        when(matchResultRepository.findByMatchId(matchId)).thenReturn(Optional.empty());
        when(matchResultRepository.save(any(MatchResult.class))).thenReturn(matchResult);


        MatchResult result = matchResultService.initializeMatchResult(match);


        assertNotNull(result);
        assertEquals(MatchResultStatus.PENDING, result.getStatus());


        verify(matchResultRepository).findByMatchId(matchId);
        verify(matchResultRepository).save(any(MatchResult.class));
    }

    @Test
    void initializeMatchResult_ResultExists_ReturnsExistingResult() {

        when(matchResultRepository.findByMatchId(matchId)).thenReturn(Optional.of(matchResult));


        MatchResult result = matchResultService.initializeMatchResult(match);


        assertNotNull(result);
        assertEquals(resultId, result.getId());


        verify(matchResultRepository).findByMatchId(matchId);
        verify(matchResultRepository, never()).save(any(MatchResult.class));
    }

    @Test
    void submitMatchScore_Success() {

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.existsByMatchIdAndPlayerId(matchId, userId)).thenReturn(true);
        when(matchResultRepository.findByMatchId(matchId)).thenReturn(Optional.of(matchResult));
        when(scoreSubmissionRepository.findByMatchIdAndSubmitterId(matchId, userId)).thenReturn(Optional.empty());
        when(matchTeamRepository.findById(team1Id)).thenReturn(Optional.of(team1));
        when(matchTeamRepository.findById(team2Id)).thenReturn(Optional.of(team2));
        when(scoreSubmissionRepository.save(any(MatchScoreSubmission.class))).thenReturn(scoreSubmission);


        when(scoreSubmissionRepository.findByMatchId(matchId)).thenReturn(Arrays.asList(scoreSubmission));
        when(matchTeamRepository.findByMatchId(matchId)).thenReturn(Arrays.asList(team1, team2));
        when(matchResultRepository.save(any(MatchResult.class))).thenReturn(matchResult);


        MatchScoreSubmission result = matchResultService.submitMatchScore(
                matchId, userId, team1Id, team2Id, 3, 1);


        assertNotNull(result);
        assertEquals(userId, result.getSubmitterId());
        assertEquals(team1, result.getTeam1());
        assertEquals(team2, result.getTeam2());
        assertEquals(3, result.getTeam1Score());
        assertEquals(1, result.getTeam2Score());


        verify(matchRepository).findById(matchId);
        verify(matchPlayerRepository).existsByMatchIdAndPlayerId(matchId, userId);
        verify(scoreSubmissionRepository).findByMatchIdAndSubmitterId(matchId, userId);
        verify(matchTeamRepository).findById(team1Id);
        verify(matchTeamRepository).findById(team2Id);
        verify(scoreSubmissionRepository).save(any(MatchScoreSubmission.class));


        verify(scoreSubmissionRepository).findByMatchId(matchId);
        verify(matchTeamRepository).findByMatchId(matchId);

        ArgumentCaptor<MatchResult> resultCaptor = ArgumentCaptor.forClass(MatchResult.class);
        verify(matchResultRepository, atLeastOnce()).save(resultCaptor.capture());

        MatchResult updatedResult = resultCaptor.getValue();
        assertEquals(MatchResultStatus.TEMPORARY, updatedResult.getStatus());
        assertEquals(3, updatedResult.getTeam1Score());
        assertEquals(1, updatedResult.getTeam2Score());
        assertEquals(team1, updatedResult.getWinningTeam());
    }

    @Test
    void submitMatchScore_MatchNotFound() {

        when(matchRepository.findById(matchId)).thenReturn(Optional.empty());


        MatchException exception = assertThrows(MatchException.class, () ->
                matchResultService.submitMatchScore(matchId, userId, team1Id, team2Id, 3, 1));

        assertEquals("Match not found: " + matchId, exception.getMessage());
        assertEquals("MATCH_NOT_FOUND", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void submitMatchScore_UserNotInMatch() {

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.existsByMatchIdAndPlayerId(matchId, userId)).thenReturn(false);


        MatchException exception = assertThrows(MatchException.class, () ->
                matchResultService.submitMatchScore(matchId, userId, team1Id, team2Id, 3, 1));

        assertEquals("User did not participate in this match", exception.getMessage());
        assertEquals("USER_NOT_IN_MATCH", exception.getErrorCode());
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void submitMatchScore_ResultAlreadyConfirmed() {

        matchResult.setStatus(MatchResultStatus.CONFIRMED);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.existsByMatchIdAndPlayerId(matchId, userId)).thenReturn(true);
        when(matchResultRepository.findByMatchId(matchId)).thenReturn(Optional.of(matchResult));


        MatchException exception = assertThrows(MatchException.class, () ->
                matchResultService.submitMatchScore(matchId, userId, team1Id, team2Id, 3, 1));

        assertEquals("Match result is already confirmed", exception.getMessage());
        assertEquals("MATCH_RESULT_CONFIRMED", exception.getErrorCode());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void submitMatchScore_ScoreAlreadySubmitted() {

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.existsByMatchIdAndPlayerId(matchId, userId)).thenReturn(true);
        when(matchResultRepository.findByMatchId(matchId)).thenReturn(Optional.of(matchResult));
        when(scoreSubmissionRepository.findByMatchIdAndSubmitterId(matchId, userId))
                .thenReturn(Optional.of(scoreSubmission));


        MatchException exception = assertThrows(MatchException.class, () ->
                matchResultService.submitMatchScore(matchId, userId, team1Id, team2Id, 3, 1));

        assertEquals("User has already submitted a score for this match", exception.getMessage());
        assertEquals("SCORE_ALREADY_SUBMITTED", exception.getErrorCode());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void submitMatchScore_TeamNotFound() {

        UUID nonExistentTeamId = UUID.randomUUID();

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.existsByMatchIdAndPlayerId(matchId, userId)).thenReturn(true);
        when(matchResultRepository.findByMatchId(matchId)).thenReturn(Optional.of(matchResult));
        when(scoreSubmissionRepository.findByMatchIdAndSubmitterId(matchId, userId)).thenReturn(Optional.empty());
        when(matchTeamRepository.findById(nonExistentTeamId)).thenReturn(Optional.empty());


        MatchException exception = assertThrows(MatchException.class, () ->
                matchResultService.submitMatchScore(matchId, userId, nonExistentTeamId, team2Id, 3, 1));

        assertEquals("Team not found: " + nonExistentTeamId, exception.getMessage());
        assertEquals("TEAM_NOT_FOUND", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void submitMatchScore_TeamsNotInMatch() {

        Match otherMatch = new Match();
        otherMatch.setId(UUID.randomUUID());

        MatchTeam otherTeam = new MatchTeam();
        otherTeam.setId(UUID.randomUUID());
        otherTeam.setMatch(otherMatch);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.existsByMatchIdAndPlayerId(matchId, userId)).thenReturn(true);
        when(matchResultRepository.findByMatchId(matchId)).thenReturn(Optional.of(matchResult));
        when(scoreSubmissionRepository.findByMatchIdAndSubmitterId(matchId, userId)).thenReturn(Optional.empty());
        when(matchTeamRepository.findById(team1Id)).thenReturn(Optional.of(team1));
        when(matchTeamRepository.findById(otherTeam.getId())).thenReturn(Optional.of(otherTeam));


        MatchException exception = assertThrows(MatchException.class, () ->
                matchResultService.submitMatchScore(matchId, userId, team1Id, otherTeam.getId(), 3, 1));

        assertEquals("Teams do not belong to this match", exception.getMessage());
        assertEquals("INVALID_TEAMS", exception.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void submitMatchScore_DrawResult() {

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.existsByMatchIdAndPlayerId(matchId, userId)).thenReturn(true);
        when(matchResultRepository.findByMatchId(matchId)).thenReturn(Optional.of(matchResult));
        when(scoreSubmissionRepository.findByMatchIdAndSubmitterId(matchId, userId)).thenReturn(Optional.empty());
        when(matchTeamRepository.findById(team1Id)).thenReturn(Optional.of(team1));
        when(matchTeamRepository.findById(team2Id)).thenReturn(Optional.of(team2));

        MatchScoreSubmission drawSubmission = MatchScoreSubmission.builder()
                .match(match)
                .submitterId(userId)
                .team1(team1)
                .team2(team2)
                .team1Score(2)
                .team2Score(2)
                .status(ScoreSubmissionStatus.PENDING)
                .createdAt(ZonedDateTime.now())
                .build();

        when(scoreSubmissionRepository.save(any(MatchScoreSubmission.class))).thenReturn(drawSubmission);


        when(scoreSubmissionRepository.findByMatchId(matchId)).thenReturn(Arrays.asList(drawSubmission));
        when(matchTeamRepository.findByMatchId(matchId)).thenReturn(Arrays.asList(team1, team2));
        when(matchResultRepository.save(any(MatchResult.class))).thenReturn(matchResult);


        MatchScoreSubmission result = matchResultService.submitMatchScore(
                matchId, userId, team1Id, team2Id, 2, 2);


        assertNotNull(result);
        assertEquals(2, result.getTeam1Score());
        assertEquals(2, result.getTeam2Score());


        ArgumentCaptor<MatchResult> resultCaptor = ArgumentCaptor.forClass(MatchResult.class);
        verify(matchResultRepository, atLeastOnce()).save(resultCaptor.capture());

        MatchResult updatedResult = resultCaptor.getValue();
        assertEquals(MatchResultStatus.TEMPORARY, updatedResult.getStatus());
        assertEquals(2, updatedResult.getTeam1Score());
        assertEquals(2, updatedResult.getTeam2Score());
        assertNull(updatedResult.getWinningTeam());
    }

    @Test
    void confirmMatchResult_Success() {

        when(matchResultRepository.findById(resultId)).thenReturn(Optional.of(matchResult));
        when(matchTeamRepository.findByMatchId(matchId)).thenReturn(Arrays.asList(team1, team2));
        when(matchTeamRepository.findById(team1Id)).thenReturn(Optional.of(team1));
        when(matchResultRepository.save(any(MatchResult.class))).thenReturn(matchResult);
        when(scoreSubmissionRepository.save(any(MatchScoreSubmission.class))).thenReturn(scoreSubmission);


        MatchResult result = matchResultService.confirmMatchResult(resultId, team1Id, 3, 1);


        assertNotNull(result);


        verify(matchResultRepository).findById(resultId);
        verify(matchTeamRepository).findByMatchId(matchId);
        verify(matchTeamRepository).findById(team1Id);

        ArgumentCaptor<MatchResult> resultCaptor = ArgumentCaptor.forClass(MatchResult.class);
        verify(matchResultRepository).save(resultCaptor.capture());

        MatchResult savedResult = resultCaptor.getValue();
        assertEquals(MatchResultStatus.CONFIRMED, savedResult.getStatus());
        assertEquals(team1, savedResult.getWinningTeam());
        assertNotNull(savedResult.getConfirmedAt());

        ArgumentCaptor<MatchScoreSubmission> submissionCaptor = ArgumentCaptor.forClass(MatchScoreSubmission.class);
        verify(scoreSubmissionRepository).save(submissionCaptor.capture());

        MatchScoreSubmission systemSubmission = submissionCaptor.getValue();
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000000"), systemSubmission.getSubmitterId());
        assertEquals(ScoreSubmissionStatus.ACCEPTED, systemSubmission.getStatus());
        assertEquals(3, systemSubmission.getTeam1Score());
        assertEquals(1, systemSubmission.getTeam2Score());
    }

    @Test
    void confirmMatchResult_ResultNotFound() {

        when(matchResultRepository.findById(resultId)).thenReturn(Optional.empty());


        MatchException exception = assertThrows(MatchException.class, () ->
                matchResultService.confirmMatchResult(resultId, team1Id, 3, 1));

        assertEquals("Match result not found: " + resultId, exception.getMessage());
        assertEquals("MATCH_RESULT_NOT_FOUND", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void confirmMatchResult_InvalidTeamCount() {

        when(matchResultRepository.findById(resultId)).thenReturn(Optional.of(matchResult));
        when(matchTeamRepository.findByMatchId(matchId)).thenReturn(Arrays.asList(team1));


        MatchException exception = assertThrows(MatchException.class, () ->
                matchResultService.confirmMatchResult(resultId, team1Id, 3, 1));

        assertEquals("Match does not have exactly 2 teams", exception.getMessage());
        assertEquals("INVALID_TEAM_COUNT", exception.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void confirmMatchResult_WinningTeamNotFound() {

        UUID nonExistentTeamId = UUID.randomUUID();

        when(matchResultRepository.findById(resultId)).thenReturn(Optional.of(matchResult));
        when(matchTeamRepository.findByMatchId(matchId)).thenReturn(Arrays.asList(team1, team2));
        when(matchTeamRepository.findById(nonExistentTeamId)).thenReturn(Optional.empty());


        MatchException exception = assertThrows(MatchException.class, () ->
                matchResultService.confirmMatchResult(resultId, nonExistentTeamId, 3, 1));

        assertEquals("Team not found: " + nonExistentTeamId, exception.getMessage());
        assertEquals("TEAM_NOT_FOUND", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void confirmMatchResult_NullWinningTeam_Draw() {

        when(matchResultRepository.findById(resultId)).thenReturn(Optional.of(matchResult));
        when(matchTeamRepository.findByMatchId(matchId)).thenReturn(Arrays.asList(team1, team2));
        when(matchResultRepository.save(any(MatchResult.class))).thenReturn(matchResult);
        when(scoreSubmissionRepository.save(any(MatchScoreSubmission.class))).thenReturn(scoreSubmission);


        MatchResult result = matchResultService.confirmMatchResult(resultId, null, 2, 2);


        assertNotNull(result);


        ArgumentCaptor<MatchResult> resultCaptor = ArgumentCaptor.forClass(MatchResult.class);
        verify(matchResultRepository).save(resultCaptor.capture());

        MatchResult savedResult = resultCaptor.getValue();
        assertEquals(MatchResultStatus.CONFIRMED, savedResult.getStatus());
        assertNull(savedResult.getWinningTeam());
    }

    @Test
    void confirmMatchResult_NullScores_OnlyStatusUpdated() {

        when(matchResultRepository.findById(resultId)).thenReturn(Optional.of(matchResult));
        when(matchTeamRepository.findByMatchId(matchId)).thenReturn(Arrays.asList(team1, team2));
        when(matchTeamRepository.findById(team1Id)).thenReturn(Optional.of(team1));
        when(matchResultRepository.save(any(MatchResult.class))).thenReturn(matchResult);


        MatchResult result = matchResultService.confirmMatchResult(resultId, team1Id, null, null);


        assertNotNull(result);


        verify(matchResultRepository).save(any(MatchResult.class));
        verify(scoreSubmissionRepository, never()).save(any(MatchScoreSubmission.class));
    }

    @Test
    void hasUserSubmittedScore_True() {

        when(scoreSubmissionRepository.findByMatchIdAndSubmitterId(matchId, userId))
                .thenReturn(Optional.of(scoreSubmission));


        boolean result = matchResultService.hasUserSubmittedScore(matchId, userId);


        assertTrue(result);
    }

    @Test
    void hasUserSubmittedScore_False() {

        when(scoreSubmissionRepository.findByMatchIdAndSubmitterId(matchId, userId))
                .thenReturn(Optional.empty());


        boolean result = matchResultService.hasUserSubmittedScore(matchId, userId);


        assertFalse(result);
    }

    @Test
    void getMatchResult_Success() {

        when(matchResultRepository.findByMatchId(matchId)).thenReturn(Optional.of(matchResult));


        MatchResult result = matchResultService.getMatchResult(matchId);


        assertNotNull(result);
        assertEquals(resultId, result.getId());
    }

    @Test
    void getMatchResult_NotFound() {

        when(matchResultRepository.findByMatchId(matchId)).thenReturn(Optional.empty());


        MatchException exception = assertThrows(MatchException.class, () ->
                matchResultService.getMatchResult(matchId));

        assertEquals("Match result not found for match: " + matchId, exception.getMessage());
        assertEquals("MATCH_RESULT_NOT_FOUND", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
}