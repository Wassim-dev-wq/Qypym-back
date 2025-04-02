package org.fivy.matchservice.domain.repository;

import org.fivy.matchservice.domain.entity.MatchFeedbackRequest;
import org.fivy.matchservice.domain.enums.FeedbackRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchFeedbackRequestRepository extends JpaRepository<MatchFeedbackRequest, UUID> {

    Optional<MatchFeedbackRequest> findByMatchId(UUID matchId);

    @Query("SELECT mfr FROM MatchFeedbackRequest mfr WHERE mfr.status = 'PENDING' AND mfr.expiryAt < :now")
    List<MatchFeedbackRequest> findExpiredRequests(@Param("now") ZonedDateTime now);

    List<MatchFeedbackRequest> findByStatus(FeedbackRequestStatus status);

    void deleteByMatchId(UUID matchId);
}