package org.fivy.matchservice.domain.repository;

import org.fivy.matchservice.domain.entity.PlayerMatchFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerMatchFeedbackRepository extends JpaRepository<PlayerMatchFeedback, UUID> {

    Optional<PlayerMatchFeedback> findByFeedbackRequestIdAndPlayerId(UUID feedbackRequestId, UUID playerId);

    @Query("SELECT pmf FROM PlayerMatchFeedback pmf WHERE pmf.feedbackRequest.match.id = :matchId")
    List<PlayerMatchFeedback> findByMatchId(@Param("matchId") UUID matchId);

    List<PlayerMatchFeedback> findByPlayerId(UUID playerId);

    @Query("SELECT COUNT(pmf) FROM PlayerMatchFeedback pmf WHERE pmf.feedbackRequest.id = :requestId")
    long countByFeedbackRequestId(@Param("requestId") UUID requestId);

    void deleteByFeedbackRequestIdIn(List<UUID> collect);
}