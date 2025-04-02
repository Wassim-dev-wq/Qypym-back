package org.fivy.matchservice.application.service;

import org.fivy.matchservice.domain.entity.Match;
import org.fivy.matchservice.domain.entity.MatchResult;
import org.fivy.matchservice.domain.entity.MatchScoreSubmission;

import java.util.UUID;

public interface MatchResultService {

    MatchResult initializeMatchResult(Match match);

    MatchScoreSubmission submitMatchScore(
            UUID matchId,
            UUID submitterId,
            UUID team1Id,
            UUID team2Id,
            Integer team1Score,
            Integer team2Score);

    MatchResult confirmMatchResult(
            UUID resultId,
            UUID winningTeamId,
            Integer team1Score,
            Integer team2Score);

    boolean hasUserSubmittedScore(UUID matchId, UUID userId);

    MatchResult getMatchResult(UUID matchId);
}