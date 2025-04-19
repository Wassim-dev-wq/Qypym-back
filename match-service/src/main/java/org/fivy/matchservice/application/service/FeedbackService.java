package org.fivy.matchservice.application.service;

import org.fivy.matchservice.api.dto.request.SubmitMatchFeedbackRequest;
import org.fivy.matchservice.api.dto.response.FeedbackRequestResponse;
import org.fivy.matchservice.api.dto.response.PlayerRatingSummaryResponse;
import org.fivy.matchservice.domain.entity.Match;

import java.util.List;
import java.util.UUID;

public interface FeedbackService {


    FeedbackRequestResponse createFeedbackRequest(Match match);

    FeedbackRequestResponse getFeedbackRequest(UUID matchId, UUID currentUserId);

    FeedbackRequestResponse submitFeedback(
            UUID feedbackRequestId,
            UUID currentUserId,
            SubmitMatchFeedbackRequest request
    );

    PlayerRatingSummaryResponse getPlayerRatingSummary(UUID playerId);

    List<PlayerRatingSummaryResponse> getTopRatedPlayers(int limit);

    void processExpiredFeedbackRequests();

    void updatePlayerRatingSummaries(UUID playerId);
}