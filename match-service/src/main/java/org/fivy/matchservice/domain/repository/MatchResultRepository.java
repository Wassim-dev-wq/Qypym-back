package org.fivy.matchservice.domain.repository;

import org.fivy.matchservice.domain.entity.MatchResult;
import org.fivy.matchservice.domain.enums.MatchResultStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchResultRepository extends JpaRepository<MatchResult, UUID> {
    Optional<MatchResult> findByMatchId(UUID matchId);
    @Query("SELECT mr FROM MatchResult mr JOIN mr.match m WHERE mr.status = :status AND m.startDate < :cutoffTime")
    List<MatchResult> findPendingResultsForCompletedMatches(
            @Param("status") MatchResultStatus status,
            @Param("cutoffTime") ZonedDateTime cutoffTime);

    @Query(value = "SELECT mr.* FROM match_results mr " +
            "JOIN matches m ON mr.match_id = m.id " +
            "WHERE mr.status = :status " +
            "AND (m.start_date + (m.duration * INTERVAL '1 minute')) < :cutoffTime",
            nativeQuery = true)
    List<MatchResult> findPendingResultsForCompletedMatchesNative(
            @Param("status") String status,
            @Param("cutoffTime") ZonedDateTime cutoffTime);

    void deleteByMatchId(UUID matchId);
}