package org.fivy.matchservice.domain.repository;

import org.fivy.matchservice.domain.entity.PlayerRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlayerRatingRepository extends JpaRepository<PlayerRating, UUID> {

    List<PlayerRating> findByRatedPlayerId(UUID ratedPlayerId);

    @Query("SELECT pr FROM PlayerRating pr WHERE pr.feedback.feedbackRequest.match.id = :matchId")
    List<PlayerRating> findByMatchId(@Param("matchId") UUID matchId);

    @Query("SELECT pr FROM PlayerRating pr WHERE pr.feedback.feedbackRequest.match.id = :matchId AND pr.ratedPlayerId = :playerId")
    List<PlayerRating> findByMatchIdAndRatedPlayerId(
            @Param("matchId") UUID matchId,
            @Param("playerId") UUID playerId
    );

    @Query("SELECT pr FROM PlayerRating pr WHERE pr.feedback.feedbackRequest.id = :feedbackRequestId AND pr.ratedPlayerId = :playerId")
    List<PlayerRating> findByFeedbackRequestIdAndRatedPlayerId(UUID feedbackRequestId, UUID playerId);

    void deleteByFeedbackFeedbackRequestIdIn(List<UUID> feedbackRequestIds);
}