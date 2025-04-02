package org.fivy.matchservice.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.application.service.MatchResultService;
import org.fivy.matchservice.domain.entity.Match;
import org.fivy.matchservice.domain.entity.MatchResult;
import org.fivy.matchservice.domain.entity.MatchScoreSubmission;
import org.fivy.matchservice.domain.entity.MatchTeam;
import org.fivy.matchservice.domain.enums.MatchResultStatus;
import org.fivy.matchservice.domain.enums.ScoreSubmissionStatus;
import org.fivy.matchservice.domain.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MatchResultServiceImpl implements MatchResultService {

    private final MatchRepository matchRepository;
    private final MatchTeamRepository matchTeamRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchResultRepository matchResultRepository;
    private final MatchScoreSubmissionRepository scoreSubmissionRepository;

    @Value("${match.score.consensus-threshold:50}")
    private int consensusThresholdPercent;

    @Value("${match.score.min-submissions:2}")
    private int minSubmissionsRequired;

    @Override
    public MatchResult initializeMatchResult(Match match) {
        Optional<MatchResult> existingResult = matchResultRepository.findByMatchId(match.getId());

        if (existingResult.isPresent()) {
            return existingResult.get();
        }

        MatchResult result = MatchResult.builder()
                .match(match)
                .status(MatchResultStatus.PENDING)
                .createdAt(ZonedDateTime.now())
                .build();

        MatchResult savedResult = matchResultRepository.save(result);
        log.info("Initialized result for match: {}", match.getId());

        return savedResult;
    }

    @Override
    public MatchScoreSubmission submitMatchScore(
            UUID matchId,
            UUID submitterId,
            UUID team1Id,
            UUID team2Id,
            Integer team1Score,
            Integer team2Score) {

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new org.fivy.matchservice.shared.exception.MatchException(
                        "Match not found: " + matchId,
                        "MATCH_NOT_FOUND",
                        HttpStatus.NOT_FOUND
                ));

        if (!matchPlayerRepository.existsByMatchIdAndPlayerId(matchId, submitterId)) {
            throw new org.fivy.matchservice.shared.exception.MatchException(
                    "User did not participate in this match",
                    "USER_NOT_IN_MATCH",
                    HttpStatus.FORBIDDEN
            );
        }
        MatchResult result = matchResultRepository.findByMatchId(matchId)
                .orElseGet(() -> initializeMatchResult(match));
        if (result.getStatus() == MatchResultStatus.CONFIRMED) {
            throw new org.fivy.matchservice.shared.exception.MatchException(
                    "Match result is already confirmed",
                    "MATCH_RESULT_CONFIRMED",
                    HttpStatus.CONFLICT
            );
        }
        Optional<MatchScoreSubmission> existingSubmission =
                scoreSubmissionRepository.findByMatchIdAndSubmitterId(matchId, submitterId);

        if (existingSubmission.isPresent()) {
            throw new org.fivy.matchservice.shared.exception.MatchException(
                    "User has already submitted a score for this match",
                    "SCORE_ALREADY_SUBMITTED",
                    HttpStatus.CONFLICT
            );
        }
        MatchTeam team1 = matchTeamRepository.findById(team1Id)
                .orElseThrow(() -> new org.fivy.matchservice.shared.exception.MatchException(
                        "Team not found: " + team1Id,
                        "TEAM_NOT_FOUND",
                        HttpStatus.NOT_FOUND
                ));

        MatchTeam team2 = matchTeamRepository.findById(team2Id)
                .orElseThrow(() -> new org.fivy.matchservice.shared.exception.MatchException(
                        "Team not found: " + team2Id,
                        "TEAM_NOT_FOUND",
                        HttpStatus.NOT_FOUND
                ));

        if (!team1.getMatch().getId().equals(matchId) || !team2.getMatch().getId().equals(matchId)) {
            throw new org.fivy.matchservice.shared.exception.MatchException(
                    "Teams do not belong to this match",
                    "INVALID_TEAMS",
                    HttpStatus.BAD_REQUEST
            );
        }
        MatchScoreSubmission submission = MatchScoreSubmission.builder()
                .match(match)
                .submitterId(submitterId)
                .team1(team1)
                .team2(team2)
                .team1Score(team1Score)
                .team2Score(team2Score)
                .status(ScoreSubmissionStatus.PENDING)
                .createdAt(ZonedDateTime.now())
                .build();

        MatchScoreSubmission savedSubmission = scoreSubmissionRepository.save(submission);
        updateTemporaryScores(matchId);

        log.info("User {} submitted score for match {}: {} - {}, temporary scores updated",
                submitterId, matchId, team1Score, team2Score);

        return savedSubmission;
    }

    private void updateTemporaryScores(UUID matchId) {
        List<MatchScoreSubmission> submissions = scoreSubmissionRepository.findByMatchId(matchId);

        if (submissions.isEmpty()) {
            return;
        }

        MatchResult result = matchResultRepository.findByMatchId(matchId)
                .orElseThrow(() -> new org.fivy.matchservice.shared.exception.MatchException(
                        "Match result not found for match: " + matchId,
                        "MATCH_RESULT_NOT_FOUND",
                        HttpStatus.NOT_FOUND
                ));

        if (result.getStatus() == MatchResultStatus.CONFIRMED) {
            return;
        }

        List<MatchTeam> teams = matchTeamRepository.findByMatchId(matchId);
        if (teams.size() != 2) {
            log.error("Match {} does not have exactly 2 teams, cannot update scores", matchId);
            return;
        }

        MatchTeam team1 = teams.get(0);
        MatchTeam team2 = teams.get(1);

        double team1AvgScore = calculateAverageScore(submissions, team1);
        double team2AvgScore = calculateAverageScore(submissions, team2);
        int team1RoundedScore = (int) Math.round(team1AvgScore);
        int team2RoundedScore = (int) Math.round(team2AvgScore);

        MatchTeam tempWinningTeam = null;
        if (team1RoundedScore > team2RoundedScore) {
            tempWinningTeam = team1;
        } else if (team2RoundedScore > team1RoundedScore) {
            tempWinningTeam = team2;
        }

        result.setTeam1Score(team1RoundedScore);
        result.setTeam2Score(team2RoundedScore);
        result.setWinningTeam(tempWinningTeam);
        result.setStatus(MatchResultStatus.TEMPORARY);
        result.setUpdatedAt(ZonedDateTime.now());

        matchResultRepository.save(result);

        log.info("Updated temporary scores for match {}: {} - {}",
                matchId, team1RoundedScore, team2RoundedScore);
    }

    private double calculateAverageScore(List<MatchScoreSubmission> submissions, MatchTeam team) {
        int totalScore = 0;
        int count = 0;

        for (MatchScoreSubmission submission : submissions) {
            if (submission.getTeam1().getId().equals(team.getId())) {
                totalScore += submission.getTeam1Score();
                count++;
            } else if (submission.getTeam2().getId().equals(team.getId())) {
                totalScore += submission.getTeam2Score();
                count++;
            }
        }

        return count > 0 ? (double) totalScore / count : 0;
    }

    @Override
    public MatchResult confirmMatchResult(UUID resultId, UUID winningTeamId,
                                          Integer team1Score, Integer team2Score) {
        MatchResult result = matchResultRepository.findById(resultId)
                .orElseThrow(() -> new org.fivy.matchservice.shared.exception.MatchException(
                        "Match result not found: " + resultId,
                        "MATCH_RESULT_NOT_FOUND",
                        HttpStatus.NOT_FOUND
                ));

        List<MatchTeam> teams = matchTeamRepository.findByMatchId(result.getMatch().getId());

        if (teams.size() != 2) {
            throw new org.fivy.matchservice.shared.exception.MatchException(
                    "Match does not have exactly 2 teams",
                    "INVALID_TEAM_COUNT",
                    HttpStatus.BAD_REQUEST
            );
        }

        MatchTeam team1 = teams.get(0);
        MatchTeam team2 = teams.get(1);

        MatchTeam winningTeam = null;
        if (winningTeamId != null) {
            winningTeam = matchTeamRepository.findById(winningTeamId)
                    .orElseThrow(() -> new org.fivy.matchservice.shared.exception.MatchException(
                            "Team not found: " + winningTeamId,
                            "TEAM_NOT_FOUND",
                            HttpStatus.NOT_FOUND
                    ));
        }

        result.setWinningTeam(winningTeam);
        result.setStatus(MatchResultStatus.CONFIRMED);
        result.setConfirmedAt(ZonedDateTime.now());
        result.setUpdatedAt(ZonedDateTime.now());

        MatchResult updatedResult = matchResultRepository.save(result);
        log.info("Admin manually confirmed result for match: {}", result.getMatch().getId());
        if (team1Score != null && team2Score != null) {
            UUID systemUserId = UUID.fromString("00000000-0000-0000-0000-000000000000");

            MatchScoreSubmission officialSubmission = MatchScoreSubmission.builder()
                    .match(result.getMatch())
                    .submitterId(systemUserId)
                    .team1(team1)
                    .team2(team2)
                    .team1Score(team1Score)
                    .team2Score(team2Score)
                    .status(ScoreSubmissionStatus.ACCEPTED)
                    .createdAt(ZonedDateTime.now())
                    .build();

            scoreSubmissionRepository.save(officialSubmission);
        }

        return updatedResult;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserSubmittedScore(UUID matchId, UUID userId) {
        return scoreSubmissionRepository.findByMatchIdAndSubmitterId(matchId, userId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public MatchResult getMatchResult(UUID matchId) {
        return matchResultRepository.findByMatchId(matchId)
                .orElseThrow(() -> new org.fivy.matchservice.shared.exception.MatchException(
                        "Match result not found for match: " + matchId,
                        "MATCH_RESULT_NOT_FOUND",
                        HttpStatus.NOT_FOUND
                ));
    }

    private int calculateRequiredSubmissions(UUID matchId) {
        long playerCount = matchPlayerRepository.countByMatchId(matchId);
        return Math.max(minSubmissionsRequired, (int) Math.ceil(playerCount * 0.5));
    }

    private static class ScoreKey {
        private final UUID team1Id;
        private final UUID team2Id;
        private final int team1Score;
        private final int team2Score;
        public ScoreKey(UUID team1Id, UUID team2Id, int team1Score, int team2Score) {
            this.team1Id = team1Id;
            this.team2Id = team2Id;
            this.team1Score = team1Score;
            this.team2Score = team2Score;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ScoreKey scoreKey = (ScoreKey) o;
            return team1Score == scoreKey.team1Score &&
                    team2Score == scoreKey.team2Score &&
                    team1Id.equals(scoreKey.team1Id) &&
                    team2Id.equals(scoreKey.team2Id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(team1Id, team2Id, team1Score, team2Score);
        }
    }
}