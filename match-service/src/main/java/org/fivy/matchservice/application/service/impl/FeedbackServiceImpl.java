package org.fivy.matchservice.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.api.dto.request.SubmitMatchFeedbackRequest;
import org.fivy.matchservice.api.dto.response.FeedbackRequestResponse;
import org.fivy.matchservice.api.dto.response.PlayerRatingSummaryResponse;
import org.fivy.matchservice.api.mapper.FeedbackMapper;
import org.fivy.matchservice.application.service.FeedbackService;
import org.fivy.matchservice.application.service.MatchResultService;
import org.fivy.matchservice.domain.entity.*;
import org.fivy.matchservice.domain.enums.FeedbackRequestStatus;
import org.fivy.matchservice.domain.repository.*;
import org.fivy.matchservice.shared.exception.FeedbackException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class FeedbackServiceImpl implements FeedbackService {

    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchFeedbackRequestRepository feedbackRequestRepository;
    private final PlayerMatchFeedbackRepository playerFeedbackRepository;
    private final PlayerRatingRepository playerRatingRepository;
    private final PlayerRatingSummaryRepository ratingSummaryRepository;
    private final FeedbackMapper feedbackMapper;

    private final MatchResultService matchResultService;
    private static final long FEEDBACK_WINDOW_HOURS = 48;

    @Override
    public FeedbackRequestResponse createFeedbackRequest(Match match) {
        if (feedbackRequestRepository.findByMatchId(match.getId()).isPresent()) {
            throw new FeedbackException(
                    "Feedback request already exists for this match",
                    "FEEDBACK_REQUEST_EXISTS",
                    HttpStatus.CONFLICT
            );
        }
        MatchFeedbackRequest request = MatchFeedbackRequest.builder()
                .match(match)
                .status(FeedbackRequestStatus.PENDING)
                .createdAt(ZonedDateTime.now())
                .expiryAt(ZonedDateTime.now().plusHours(FEEDBACK_WINDOW_HOURS))
                .build();
        MatchFeedbackRequest savedRequest = feedbackRequestRepository.save(request);
        FeedbackRequestResponse response = feedbackMapper.toFeedbackRequestResponse(savedRequest);
        response.setTotalPlayersInMatch(matchPlayerRepository.countByMatchId(match.getId()));
        response.setFeedbackCount(0);
        log.info("Created feedback request for match: {}", match.getId());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackRequestResponse getFeedbackRequest(UUID matchId, UUID currentUserId) {
        MatchFeedbackRequest request = feedbackRequestRepository.findByMatchId(matchId)
                .orElseThrow(() -> new FeedbackException(
                        "Feedback request not found for match: " + matchId,
                        "FEEDBACK_REQUEST_NOT_FOUND",
                        HttpStatus.NOT_FOUND
                ));
        FeedbackRequestResponse response = feedbackMapper.toFeedbackRequestResponse(request);
        response.setTotalPlayersInMatch(matchPlayerRepository.countByMatchId(matchId));
        response.setFeedbackCount((int) playerFeedbackRepository.countByFeedbackRequestId(request.getId()));
        response.setUserHasSubmitted(
                playerFeedbackRepository.findByFeedbackRequestIdAndPlayerId(request.getId(), currentUserId).isPresent()
        );
        return response;
    }

    @Override
    public FeedbackRequestResponse submitFeedback(
            UUID feedbackRequestId,
            UUID currentUserId,
            SubmitMatchFeedbackRequest request) {
        MatchFeedbackRequest feedbackRequest = feedbackRequestRepository.findById(feedbackRequestId)
                .orElseThrow(() -> new FeedbackException(
                        "Feedback request not found: " + feedbackRequestId,
                        "FEEDBACK_REQUEST_NOT_FOUND",
                        HttpStatus.NOT_FOUND
                ));
        if (feedbackRequest.getStatus() != FeedbackRequestStatus.PENDING) {
            throw new FeedbackException(
                    "Feedback request is no longer accepting submissions",
                    "FEEDBACK_REQUEST_CLOSED",
                    HttpStatus.BAD_REQUEST
            );
        }
        Optional<PlayerMatchFeedback> existingFeedback =
                playerFeedbackRepository.findByFeedbackRequestIdAndPlayerId(feedbackRequestId, currentUserId);
        if (existingFeedback.isPresent()) {
            throw new FeedbackException(
                    "Feedback already submitted",
                    "FEEDBACK_ALREADY_SUBMITTED",
                    HttpStatus.CONFLICT
            );
        }
        if (!matchPlayerRepository.existsByMatchIdAndPlayerId(
                feedbackRequest.getMatch().getId(), currentUserId)) {
            throw new FeedbackException(
                    "User did not participate in this match",
                    "USER_NOT_IN_MATCH",
                    HttpStatus.FORBIDDEN
            );
        }
        PlayerMatchFeedback feedback = PlayerMatchFeedback.builder()
                .feedbackRequest(feedbackRequest)
                .playerId(currentUserId)
                .matchRating(request.getMatchRating())
                .matchComments(request.getMatchComments())
                .submittedAt(ZonedDateTime.now())
                .build();

        PlayerMatchFeedback savedFeedback = playerFeedbackRepository.save(feedback);
        if (request.getPlayerRatings() != null && !request.getPlayerRatings().isEmpty()) {
            savePlayerRatings(savedFeedback, currentUserId, request.getPlayerRatings());
        }

        if (request.getTeam1Id() != null && request.getTeam2Id() != null &&
                request.getTeam1Score() != null && request.getTeam2Score() != null) {

            try {
                matchResultService.submitMatchScore(
                        feedbackRequest.getMatch().getId(),
                        currentUserId,
                        request.getTeam1Id(),
                        request.getTeam2Id(),
                        request.getTeam1Score(),
                        request.getTeam2Score()
                );
                log.info("Match score submitted along with feedback for match: {}",
                        feedbackRequest.getMatch().getId());
            } catch (Exception e) {
                log.error("Error submitting match score: {}", e.getMessage());
            }
        }
        long submittedCount = playerFeedbackRepository.countByFeedbackRequestId(feedbackRequestId);
        long totalPlayers = matchPlayerRepository.countByMatchId(feedbackRequest.getMatch().getId());
        if (submittedCount >= totalPlayers) {
            feedbackRequest.setStatus(FeedbackRequestStatus.COMPLETED);
            feedbackRequestRepository.save(feedbackRequest);
        }
        for (SubmitMatchFeedbackRequest.PlayerRatingRequest ratingRequest : request.getPlayerRatings()) {
            updatePlayerRatingSummaries(ratingRequest.getRatedPlayerId());
        }
        return getFeedbackRequest(feedbackRequest.getMatch().getId(), currentUserId);
    }

    private void savePlayerRatings(
            PlayerMatchFeedback feedback,
            UUID ratingPlayerId,
            List<SubmitMatchFeedbackRequest.PlayerRatingRequest> ratingRequests) {

        for (SubmitMatchFeedbackRequest.PlayerRatingRequest ratingRequest : ratingRequests) {
            if (!matchPlayerRepository.existsByMatchIdAndPlayerId(
                    feedback.getFeedbackRequest().getMatch().getId(),
                    ratingRequest.getRatedPlayerId())) {
                log.warn("Skipping rating for player {} who did not participate in match {}",
                        ratingRequest.getRatedPlayerId(),
                        feedback.getFeedbackRequest().getMatch().getId());
                continue;
            }

            if (ratingPlayerId.equals(ratingRequest.getRatedPlayerId())) {
                log.warn("Skipping self-rating attempt by player {}", ratingPlayerId);
                continue;
            }

            PlayerRating rating = feedbackMapper.toPlayerRatingEntity(ratingRequest);
            rating.setFeedback(feedback);
            rating.setRatedPlayerId(ratingRequest.getRatedPlayerId());
            rating.setRatingPlayerId(ratingPlayerId);

            playerRatingRepository.save(rating);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerRatingSummaryResponse getPlayerRatingSummary(UUID playerId) {
        PlayerRatingSummary summary = ratingSummaryRepository.findById(playerId)
                .orElseGet(() -> PlayerRatingSummary.builder()
                        .playerId(playerId)
                        .totalMatches(0)
                        .avgSkillRating(BigDecimal.ZERO)
                        .avgSportsmanshipRating(BigDecimal.ZERO)
                        .avgTeamworkRating(BigDecimal.ZERO)
                        .avgReliabilityRating(BigDecimal.ZERO)
                        .overallRating(BigDecimal.ZERO)
                        .totalRatings(0)
                        .lastUpdatedAt(ZonedDateTime.now())
                        .build());

        return feedbackMapper.toPlayerRatingSummaryResponse(summary);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerRatingSummaryResponse> getTopRatedPlayers(int limit) {
        List<PlayerRatingSummary> topPlayers = ratingSummaryRepository.findTop10ByOrderByOverallRatingDesc();

        return topPlayers.stream()
                .map(feedbackMapper::toPlayerRatingSummaryResponse)
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public void processExpiredFeedbackRequests() {
        List<MatchFeedbackRequest> expiredRequests =
                feedbackRequestRepository.findExpiredRequests(ZonedDateTime.now());

        log.info("Processing {} expired feedback requests", expiredRequests.size());

        for (MatchFeedbackRequest request : expiredRequests) {
            request.setStatus(FeedbackRequestStatus.EXPIRED);
            feedbackRequestRepository.save(request);

            log.info("Marked feedback request {} as expired for match {}",
                    request.getId(), request.getMatch().getId());

            List<PlayerMatchFeedback> feedbacks =
                    playerFeedbackRepository.findByMatchId(request.getMatch().getId());

            for (PlayerMatchFeedback feedback : feedbacks) {
                for (PlayerRating rating : feedback.getPlayerRatings()) {
                    updatePlayerRatingSummaries(rating.getRatedPlayerId());
                }
            }
        }
    }

    @Override
    public void updatePlayerRatingSummaries(UUID playerId) {
        if (playerId == null) {
            log.info("Updating all player rating summaries");
            return;
        }

        log.info("Updating rating summary for player: {}", playerId);

        List<PlayerRating> ratings = playerRatingRepository.findByRatedPlayerId(playerId);

        if (ratings.isEmpty()) {
            log.info("No ratings found for player: {}", playerId);
            return;
        }

        BigDecimal avgSkill = calculateAverage(ratings, PlayerRating::getSkillRating);
        BigDecimal avgSportsmanship = calculateAverage(ratings, PlayerRating::getSportsmanshipRating);
        BigDecimal avgTeamwork = calculateAverage(ratings, PlayerRating::getTeamworkRating);
        BigDecimal avgReliability = calculateAverage(ratings, PlayerRating::getReliabilityRating);

        BigDecimal overallRating = avgSkill
                .add(avgSportsmanship)
                .add(avgTeamwork)
                .add(avgReliability)
                .divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);

        PlayerRatingSummary summary = ratingSummaryRepository.findById(playerId)
                .orElse(new PlayerRatingSummary());

        summary.setPlayerId(playerId);
        summary.setAvgSkillRating(avgSkill);
        summary.setAvgSportsmanshipRating(avgSportsmanship);
        summary.setAvgTeamworkRating(avgTeamwork);
        summary.setAvgReliabilityRating(avgReliability);
        summary.setOverallRating(overallRating);
        summary.setTotalRatings(ratings.size());

        long uniqueMatches = ratings.stream()
                .map(rating -> rating.getFeedback().getFeedbackRequest().getMatch().getId())
                .distinct()
                .count();

        summary.setTotalMatches((int) uniqueMatches);
        summary.setLastUpdatedAt(ZonedDateTime.now());

        ratingSummaryRepository.save(summary);
        log.info("Updated rating summary for player: {}", playerId);
    }

    private BigDecimal calculateAverage(List<PlayerRating> ratings,
                                        java.util.function.Function<PlayerRating, Integer> getter) {
        return ratings.stream()
                .map(getter)
                .filter(rating -> rating != null)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(1, ratings.size())), 2, RoundingMode.HALF_UP);
    }
}