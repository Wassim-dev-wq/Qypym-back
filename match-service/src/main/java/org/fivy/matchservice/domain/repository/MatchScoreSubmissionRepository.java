package org.fivy.matchservice.domain.repository;

import org.fivy.matchservice.domain.entity.MatchScoreSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchScoreSubmissionRepository extends JpaRepository<MatchScoreSubmission, UUID> {
    List<MatchScoreSubmission> findByMatchId(UUID matchId);

    Optional<MatchScoreSubmission> findByMatchIdAndSubmitterId(UUID matchId, UUID submitterId);

    @Query("SELECT COUNT(s) FROM MatchScoreSubmission s WHERE s.match.id = :matchId")
    long countByMatchId(@Param("matchId") UUID matchId);

    @Query("SELECT COUNT(s) FROM MatchScoreSubmission s WHERE s.match.id = :matchId " +
            "AND s.team1Score = :team1Score AND s.team2Score = :team2Score")
    long countMatchingScores(@Param("matchId") UUID matchId,
                             @Param("team1Score") Integer team1Score,
                             @Param("team2Score") Integer team2Score);

    void deleteByMatchId(UUID matchId);
}