package org.fivy.matchservice.application.service.impl;

import org.fivy.matchservice.api.dto.request.SubmitMatchFeedbackRequest;
import org.fivy.matchservice.api.dto.response.FeedbackRequestResponse;
import org.fivy.matchservice.api.dto.response.PlayerRatingSummaryResponse;
import org.fivy.matchservice.api.mapper.FeedbackMapper;
import org.fivy.matchservice.application.service.MatchResultService;
import org.fivy.matchservice.domain.entity.*;
import org.fivy.matchservice.domain.enums.FeedbackRequestStatus;
import org.fivy.matchservice.domain.repository.*;
import org.fivy.matchservice.shared.exception.FeedbackException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FeedbackServiceImplTest {

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @Mock
    private MatchFeedbackRequestRepository feedbackRequestRepository;

    @Mock
    private PlayerMatchFeedbackRepository playerFeedbackRepository;

    @Mock
    private PlayerRatingRepository playerRatingRepository;

    @Mock
    private PlayerRatingSummaryRepository ratingSummaryRepository;

    @Mock
    private FeedbackMapper feedbackMapper;

    @Mock
    private MatchResultService matchResultService;

    @InjectMocks
    private FeedbackServiceImpl feedbackService;

    private UUID matchId;
    private UUID playerId;
    private UUID feedbackRequestId;
    private Match match;
    private MatchFeedbackRequest feedbackRequest;
    private PlayerMatchFeedback playerFeedback;
    private FeedbackRequestResponse feedbackRequestResponse;
    private PlayerRatingSummary playerRatingSummary;
    private PlayerRatingSummaryResponse playerRatingSummaryResponse;
    private SubmitMatchFeedbackRequest submitRequest;

    @BeforeEach
    void setUp() {
        matchId = UUID.randomUUID();
        playerId = UUID.randomUUID();
        feedbackRequestId = UUID.randomUUID();


        match = new Match();
        match.setId(matchId);


        feedbackRequest = MatchFeedbackRequest.builder()
                .id(feedbackRequestId)
                .match(match)
                .status(FeedbackRequestStatus.PENDING)
                .createdAt(ZonedDateTime.now())
                .expiryAt(ZonedDateTime.now().plusHours(48))
                .build();


        playerFeedback = PlayerMatchFeedback.builder()
                .id(UUID.randomUUID())
                .feedbackRequest(feedbackRequest)
                .playerId(playerId)
                .matchRating(4)
                .matchComments("Good match")
                .submittedAt(ZonedDateTime.now())
                .playerRatings(new HashSet<>())
                .build();


        feedbackRequestResponse = new FeedbackRequestResponse();
        feedbackRequestResponse.setId(feedbackRequestId);
        feedbackRequestResponse.setMatchId(matchId);
        feedbackRequestResponse.setStatus(FeedbackRequestStatus.PENDING);
        feedbackRequestResponse.setExpiryAt(feedbackRequest.getExpiryAt());
        feedbackRequestResponse.setTotalPlayersInMatch(10);
        feedbackRequestResponse.setFeedbackCount(5);
        feedbackRequestResponse.setUserHasSubmitted(false);


        playerRatingSummary = PlayerRatingSummary.builder()
                .playerId(playerId)
                .avgSkillRating(new BigDecimal("4.5"))
                .avgSportsmanshipRating(new BigDecimal("4.2"))
                .avgTeamworkRating(new BigDecimal("4.7"))
                .avgReliabilityRating(new BigDecimal("4.3"))
                .overallRating(new BigDecimal("4.425"))
                .totalMatches(5)
                .totalRatings(15)
                .lastUpdatedAt(ZonedDateTime.now())
                .build();


        ZonedDateTime lastUpdated = ZonedDateTime.now();
        playerRatingSummaryResponse = PlayerRatingSummaryResponse.builder()
                .playerId(playerId)
                .skillRating(new BigDecimal("4.5"))
                .sportsmanshipRating(new BigDecimal("4.2"))
                .teamworkRating(new BigDecimal("4.7"))
                .reliabilityRating(new BigDecimal("4.3"))
                .overallRating(new BigDecimal("4.425"))
                .totalMatches(5)
                .totalRatings(15)
                .lastUpdatedAt(lastUpdated)
                .build();


        UUID ratedPlayerId1 = UUID.randomUUID();
        UUID ratedPlayerId2 = UUID.randomUUID();
        UUID team1Id = UUID.randomUUID();
        UUID team2Id = UUID.randomUUID();

        SubmitMatchFeedbackRequest.PlayerRatingRequest ratingRequest1 = new SubmitMatchFeedbackRequest.PlayerRatingRequest();
        ratingRequest1.setRatedPlayerId(ratedPlayerId1);
        ratingRequest1.setSkillRating(4);
        ratingRequest1.setSportsmanshipRating(5);
        ratingRequest1.setTeamworkRating(4);
        ratingRequest1.setReliabilityRating(5);
        ratingRequest1.setComments("Great player");

        SubmitMatchFeedbackRequest.PlayerRatingRequest ratingRequest2 = new SubmitMatchFeedbackRequest.PlayerRatingRequest();
        ratingRequest2.setRatedPlayerId(ratedPlayerId2);
        ratingRequest2.setSkillRating(3);
        ratingRequest2.setSportsmanshipRating(4);
        ratingRequest2.setTeamworkRating(4);
        ratingRequest2.setReliabilityRating(3);
        ratingRequest2.setComments("Good team player");

        submitRequest = new SubmitMatchFeedbackRequest();
        submitRequest.setMatchRating(4);
        submitRequest.setMatchComments("It was a fun match");
        submitRequest.setPlayerRatings(Arrays.asList(ratingRequest1, ratingRequest2));
        submitRequest.setTeam1Id(team1Id);
        submitRequest.setTeam2Id(team2Id);
        submitRequest.setTeam1Score(3);
        submitRequest.setTeam2Score(2);
    }

    @Test
    void createFeedbackRequest_Success() {

        when(feedbackRequestRepository.findByMatchId(matchId)).thenReturn(Optional.empty());
        when(feedbackRequestRepository.save(any(MatchFeedbackRequest.class))).thenReturn(feedbackRequest);
        when(feedbackMapper.toFeedbackRequestResponse(any(MatchFeedbackRequest.class))).thenReturn(feedbackRequestResponse);
        when(matchPlayerRepository.countByMatchId(matchId)).thenReturn(10);


        FeedbackRequestResponse result = feedbackService.createFeedbackRequest(match);


        assertNotNull(result);
        assertEquals(feedbackRequestId, result.getId());
        assertEquals(10, result.getTotalPlayersInMatch());
        assertEquals(0, result.getFeedbackCount());


        verify(feedbackRequestRepository).findByMatchId(matchId);
        verify(feedbackRequestRepository).save(any(MatchFeedbackRequest.class));
        verify(feedbackMapper).toFeedbackRequestResponse(any(MatchFeedbackRequest.class));
        verify(matchPlayerRepository).countByMatchId(matchId);
    }

    @Test
    void createFeedbackRequest_AlreadyExists() {

        when(feedbackRequestRepository.findByMatchId(matchId)).thenReturn(Optional.of(feedbackRequest));


        FeedbackException exception = assertThrows(FeedbackException.class, () ->
                feedbackService.createFeedbackRequest(match));

        assertEquals("Feedback request already exists for this match", exception.getMessage());
        assertEquals("FEEDBACK_REQUEST_EXISTS", exception.getErrorCode());


        verify(feedbackRequestRepository).findByMatchId(matchId);
        verify(feedbackRequestRepository, never()).save(any(MatchFeedbackRequest.class));
    }

    @Test
    void getFeedbackRequest_Success() {

        when(feedbackRequestRepository.findByMatchId(matchId)).thenReturn(Optional.of(feedbackRequest));
        when(feedbackMapper.toFeedbackRequestResponse(feedbackRequest)).thenReturn(feedbackRequestResponse);
        when(matchPlayerRepository.countByMatchId(matchId)).thenReturn(10);
        when(playerFeedbackRepository.countByFeedbackRequestId(feedbackRequestId)).thenReturn(5L);
        when(playerFeedbackRepository.findByFeedbackRequestIdAndPlayerId(feedbackRequestId, playerId))
                .thenReturn(Optional.empty());


        FeedbackRequestResponse result = feedbackService.getFeedbackRequest(matchId, playerId);


        assertNotNull(result);
        assertEquals(feedbackRequestId, result.getId());
        assertEquals(10, result.getTotalPlayersInMatch());
        assertEquals(5, result.getFeedbackCount());
        assertFalse(result.isUserHasSubmitted());


        verify(feedbackRequestRepository).findByMatchId(matchId);
        verify(feedbackMapper).toFeedbackRequestResponse(feedbackRequest);
        verify(matchPlayerRepository).countByMatchId(matchId);
        verify(playerFeedbackRepository).countByFeedbackRequestId(feedbackRequestId);
        verify(playerFeedbackRepository).findByFeedbackRequestIdAndPlayerId(feedbackRequestId, playerId);
    }

    @Test
    void getFeedbackRequest_UserHasSubmitted() {

        when(feedbackRequestRepository.findByMatchId(matchId)).thenReturn(Optional.of(feedbackRequest));
        when(feedbackMapper.toFeedbackRequestResponse(feedbackRequest)).thenReturn(feedbackRequestResponse);
        when(matchPlayerRepository.countByMatchId(matchId)).thenReturn(10);
        when(playerFeedbackRepository.countByFeedbackRequestId(feedbackRequestId)).thenReturn(5L);
        when(playerFeedbackRepository.findByFeedbackRequestIdAndPlayerId(feedbackRequestId, playerId))
                .thenReturn(Optional.of(playerFeedback));


        FeedbackRequestResponse result = feedbackService.getFeedbackRequest(matchId, playerId);


        assertTrue(result.isUserHasSubmitted());
    }

    @Test
    void getFeedbackRequest_NotFound() {

        when(feedbackRequestRepository.findByMatchId(matchId)).thenReturn(Optional.empty());


        FeedbackException exception = assertThrows(FeedbackException.class, () ->
                feedbackService.getFeedbackRequest(matchId, playerId));

        assertEquals("Feedback request not found for match: " + matchId, exception.getMessage());
        assertEquals("FEEDBACK_REQUEST_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void submitFeedback_NotFound() {

        when(feedbackRequestRepository.findById(feedbackRequestId)).thenReturn(Optional.empty());


        FeedbackException exception = assertThrows(FeedbackException.class, () ->
                feedbackService.submitFeedback(feedbackRequestId, playerId, submitRequest));

        assertEquals("Feedback request not found: " + feedbackRequestId, exception.getMessage());
        assertEquals("FEEDBACK_REQUEST_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void submitFeedback_RequestClosed() {

        feedbackRequest.setStatus(FeedbackRequestStatus.COMPLETED);
        when(feedbackRequestRepository.findById(feedbackRequestId)).thenReturn(Optional.of(feedbackRequest));


        FeedbackException exception = assertThrows(FeedbackException.class, () ->
                feedbackService.submitFeedback(feedbackRequestId, playerId, submitRequest));

        assertEquals("Feedback request is no longer accepting submissions", exception.getMessage());
        assertEquals("FEEDBACK_REQUEST_CLOSED", exception.getErrorCode());
    }

    @Test
    void submitFeedback_AlreadySubmitted() {

        when(feedbackRequestRepository.findById(feedbackRequestId)).thenReturn(Optional.of(feedbackRequest));
        when(playerFeedbackRepository.findByFeedbackRequestIdAndPlayerId(feedbackRequestId, playerId))
                .thenReturn(Optional.of(playerFeedback));


        FeedbackException exception = assertThrows(FeedbackException.class, () ->
                feedbackService.submitFeedback(feedbackRequestId, playerId, submitRequest));

        assertEquals("Feedback already submitted", exception.getMessage());
        assertEquals("FEEDBACK_ALREADY_SUBMITTED", exception.getErrorCode());
    }

    @Test
    void submitFeedback_UserNotInMatch() {

        when(feedbackRequestRepository.findById(feedbackRequestId)).thenReturn(Optional.of(feedbackRequest));
        when(playerFeedbackRepository.findByFeedbackRequestIdAndPlayerId(feedbackRequestId, playerId))
                .thenReturn(Optional.empty());
        when(matchPlayerRepository.existsByMatchIdAndPlayerId(matchId, playerId)).thenReturn(false);


        FeedbackException exception = assertThrows(FeedbackException.class, () ->
                feedbackService.submitFeedback(feedbackRequestId, playerId, submitRequest));

        assertEquals("User did not participate in this match", exception.getMessage());
        assertEquals("USER_NOT_IN_MATCH", exception.getErrorCode());
    }

    @Test
    void getPlayerRatingSummary_Exists() {

        when(ratingSummaryRepository.findById(playerId)).thenReturn(Optional.of(playerRatingSummary));
        when(feedbackMapper.toPlayerRatingSummaryResponse(playerRatingSummary))
                .thenReturn(playerRatingSummaryResponse);


        PlayerRatingSummaryResponse result = feedbackService.getPlayerRatingSummary(playerId);


        assertNotNull(result);
        assertEquals(playerId, result.getPlayerId());
        assertEquals(new BigDecimal("4.5"), result.getSkillRating());
        assertEquals(new BigDecimal("4.425"), result.getOverallRating());
        assertEquals(5, result.getTotalMatches());
        assertEquals(15, result.getTotalRatings());
        assertNotNull(result.getLastUpdatedAt());


        verify(ratingSummaryRepository).findById(playerId);
        verify(feedbackMapper).toPlayerRatingSummaryResponse(playerRatingSummary);
    }

    @Test
    void getPlayerRatingSummary_NotExists() {

        when(ratingSummaryRepository.findById(playerId)).thenReturn(Optional.empty());
        when(feedbackMapper.toPlayerRatingSummaryResponse(any(PlayerRatingSummary.class)))
                .thenReturn(playerRatingSummaryResponse);


        PlayerRatingSummaryResponse result = feedbackService.getPlayerRatingSummary(playerId);


        assertNotNull(result);


        verify(ratingSummaryRepository).findById(playerId);
        verify(feedbackMapper).toPlayerRatingSummaryResponse(any(PlayerRatingSummary.class));
    }

    @Test
    void getTopRatedPlayers_Success() {

        List<PlayerRatingSummary> topPlayers = Arrays.asList(
                playerRatingSummary,
                PlayerRatingSummary.builder().playerId(UUID.randomUUID()).overallRating(new BigDecimal("4.1")).build(),
                PlayerRatingSummary.builder().playerId(UUID.randomUUID()).overallRating(new BigDecimal("3.9")).build()
        );

        when(ratingSummaryRepository.findTop10ByOrderByOverallRatingDesc()).thenReturn(topPlayers);
        when(feedbackMapper.toPlayerRatingSummaryResponse(any(PlayerRatingSummary.class)))
                .thenReturn(playerRatingSummaryResponse);


        List<PlayerRatingSummaryResponse> result = feedbackService.getTopRatedPlayers(2);


        assertNotNull(result);
        assertEquals(2, result.size());


        verify(ratingSummaryRepository).findTop10ByOrderByOverallRatingDesc();
        verify(feedbackMapper, times(2)).toPlayerRatingSummaryResponse(any(PlayerRatingSummary.class));
    }

    @Test
    void processExpiredFeedbackRequests_Success() {

        List<MatchFeedbackRequest> expiredRequests = Arrays.asList(feedbackRequest);
        List<PlayerMatchFeedback> feedbacks = Arrays.asList(playerFeedback);
        PlayerRating rating = new PlayerRating();
        rating.setRatedPlayerId(UUID.randomUUID());
        playerFeedback.setPlayerRatings(Collections.singleton(rating));


        when(feedbackRequestRepository.findExpiredRequests(any(ZonedDateTime.class))).thenReturn(expiredRequests);
        when(playerFeedbackRepository.findByMatchId(matchId)).thenReturn(feedbacks);
        lenient().when(playerRatingRepository.save(any(PlayerRating.class))).thenReturn(rating);


        feedbackService.processExpiredFeedbackRequests();


        ArgumentCaptor<MatchFeedbackRequest> requestCaptor = ArgumentCaptor.forClass(MatchFeedbackRequest.class);
        verify(feedbackRequestRepository).save(requestCaptor.capture());

        MatchFeedbackRequest updatedRequest = requestCaptor.getValue();
        assertEquals(FeedbackRequestStatus.EXPIRED, updatedRequest.getStatus());


        verify(feedbackRequestRepository).findExpiredRequests(any(ZonedDateTime.class));
        verify(playerFeedbackRepository).findByMatchId(matchId);
    }

    @Test
    void updatePlayerRatingSummaries_Success() {

        UUID ratedPlayerId = UUID.randomUUID();
        List<PlayerRating> ratings = new ArrayList<>();

        PlayerRating rating1 = new PlayerRating();
        rating1.setSkillRating(4);
        rating1.setSportsmanshipRating(5);
        rating1.setTeamworkRating(4);
        rating1.setReliabilityRating(3);
        rating1.setFeedback(playerFeedback);
        ratings.add(rating1);

        PlayerRating rating2 = new PlayerRating();
        rating2.setSkillRating(5);
        rating2.setSportsmanshipRating(4);
        rating2.setTeamworkRating(5);
        rating2.setReliabilityRating(4);
        rating2.setFeedback(playerFeedback);
        ratings.add(rating2);

        when(playerRatingRepository.findByRatedPlayerId(ratedPlayerId)).thenReturn(ratings);
        when(ratingSummaryRepository.findById(ratedPlayerId)).thenReturn(Optional.empty());


        feedbackService.updatePlayerRatingSummaries(ratedPlayerId);


        ArgumentCaptor<PlayerRatingSummary> summaryCaptor = ArgumentCaptor.forClass(PlayerRatingSummary.class);
        verify(ratingSummaryRepository).save(summaryCaptor.capture());

        PlayerRatingSummary summary = summaryCaptor.getValue();
        assertEquals(ratedPlayerId, summary.getPlayerId());
        assertEquals(new BigDecimal("4.50"), summary.getAvgSkillRating());
        assertEquals(new BigDecimal("4.50"), summary.getAvgSportsmanshipRating());
        assertEquals(new BigDecimal("4.50"), summary.getAvgTeamworkRating());
        assertEquals(new BigDecimal("3.50"), summary.getAvgReliabilityRating());
        assertEquals(new BigDecimal("4.25"), summary.getOverallRating());
        assertEquals(2, summary.getTotalRatings());
        assertEquals(1, summary.getTotalMatches());


        verify(playerRatingRepository).findByRatedPlayerId(ratedPlayerId);
        verify(ratingSummaryRepository).findById(ratedPlayerId);
    }

    @Test
    void updatePlayerRatingSummaries_NoRatings() {

        UUID ratedPlayerId = UUID.randomUUID();
        when(playerRatingRepository.findByRatedPlayerId(ratedPlayerId)).thenReturn(Collections.emptyList());


        feedbackService.updatePlayerRatingSummaries(ratedPlayerId);


        verify(playerRatingRepository).findByRatedPlayerId(ratedPlayerId);
        verify(ratingSummaryRepository, never()).save(any(PlayerRatingSummary.class));
    }
}