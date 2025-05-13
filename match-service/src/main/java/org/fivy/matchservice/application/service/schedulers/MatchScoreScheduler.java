package org.fivy.matchservice.application.service.schedulers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.domain.entity.MatchResult;
import org.fivy.matchservice.domain.entity.MatchScoreSubmission;
import org.fivy.matchservice.domain.entity.MatchTeam;
import org.fivy.matchservice.domain.enums.MatchResultStatus;
import org.fivy.matchservice.domain.enums.ScoreSubmissionStatus;
import org.fivy.matchservice.domain.repository.MatchResultRepository;
import org.fivy.matchservice.domain.repository.MatchScoreSubmissionRepository;
import org.fivy.matchservice.domain.repository.MatchTeamRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchScoreScheduler {

    private final MatchResultRepository matchResultRepository;
    private final MatchScoreSubmissionRepository scoreSubmissionRepository;
    private final MatchTeamRepository matchTeamRepository;

    @Value("${match.score.min-submissions:2}")
    private int minSubmissionsRequired;

    @Scheduled(fixedRate = 60 * 60 * 1000)
    @Transactional
    public void processMatchScoreConfirmations() {
        log.info("Running scheduled task to process match score confirmations after 24h waiting period");
        ZonedDateTime cutoffTime = ZonedDateTime.now().minusHours(24 + 3);
        List<MatchResult> pendingResults;
        try {
            pendingResults = matchResultRepository.findPendingResultsForCompletedMatches(
                    MatchResultStatus.PENDING, cutoffTime);
        } catch (Exception e) {
            log.warn("Error using JPQL query, falling back to native query: {}", e.getMessage());
            try {
                pendingResults = matchResultRepository.findPendingResultsForCompletedMatchesNative(
                        MatchResultStatus.PENDING.name(), cutoffTime);
            } catch (Exception e2) {
                log.error("Both query methods failed. Unable to process match results: {}", e2.getMessage());
                return;
            }
        }

        log.info("Found {} matches with pending results ready for processing", pendingResults.size());

        for (MatchResult result : pendingResults) {
            try {
                ZonedDateTime matchEndTime = result.getMatch().getStartDate()
                        .plusMinutes(result.getMatch().getDuration());

                if (matchEndTime.plusHours(24).isAfter(ZonedDateTime.now())) {
                    log.info("Skipping match {} as it hasn't been 24h since it ended",
                            result.getMatch().getId());
                    continue;
                }

                processMatchResult(result);
            } catch (Exception e) {
                log.error("Error processing match result for match {}: {}",
                        result.getMatch().getId(), e.getMessage(), e);
            }
        }
    }
    private void processMatchResult(MatchResult result) {
        UUID matchId = result.getMatch().getId();
        List<MatchScoreSubmission> submissions = scoreSubmissionRepository.findByMatchId(matchId);

        if (submissions.isEmpty()) {
            log.info("No score submissions found for match: {}", matchId);
            return;
        }

        if (submissions.size() < minSubmissionsRequired) {
            log.info("Not enough submissions for match {}: {} (minimum required: {})",
                    matchId, submissions.size(), minSubmissionsRequired);
            result.setStatus(MatchResultStatus.DISPUTED);
            result.setUpdatedAt(ZonedDateTime.now());
            matchResultRepository.save(result);

            log.warn("Match result marked as disputed for match {}: insufficient submission count", matchId);
            return;
        }

        List<MatchTeam> teams = matchTeamRepository.findByMatchId(matchId);
        if (teams.size() != 2) {
            log.error("Match {} does not have exactly 2 teams, cannot determine scores", matchId);
            return;
        }

        MatchTeam team1 = teams.get(0);
        MatchTeam team2 = teams.get(1);

        double team1MedianScore = calculateMedianScore(submissions, team1);
        double team2MedianScore = calculateMedianScore(submissions, team2);
        int team1FinalScore = (int) Math.round(team1MedianScore);
        int team2FinalScore = (int) Math.round(team2MedianScore);
        UUID winningTeamId = null;
        if (team1FinalScore > team2FinalScore) {
            winningTeamId = team1.getId();
        } else if (team2FinalScore > team1FinalScore) {
            winningTeamId = team2.getId();
        }

        MatchTeam winningTeam = winningTeamId != null ?
                matchTeamRepository.findById(winningTeamId).orElse(null) : null;

        result.setWinningTeam(winningTeam);
        result.setStatus(MatchResultStatus.CONFIRMED);
        result.setConfirmedAt(ZonedDateTime.now());
        result.setUpdatedAt(ZonedDateTime.now());
        result.setTeam1Score(team1FinalScore);
        result.setTeam2Score(team2FinalScore);

        matchResultRepository.save(result);

        for (MatchScoreSubmission submission : submissions) {
            boolean matches = Math.round(submission.getTeam1Score()) == team1FinalScore &&
                    Math.round(submission.getTeam2Score()) == team2FinalScore;

            submission.setStatus(matches ?
                    ScoreSubmissionStatus.ACCEPTED : ScoreSubmissionStatus.REJECTED);

            scoreSubmissionRepository.save(submission);
        }

        log.info("Confirmed match result for match {} after 24h: Team1({}) {} - {} Team2({}), winner: {}",
                matchId, team1.getId(), team1FinalScore, team2FinalScore, team2.getId(),
                winningTeamId != null ? winningTeamId : "Draw");
    }

    /**
     * Calculate the median score for a specific team from all submissions
     */
    private double calculateMedianScore(List<MatchScoreSubmission> submissions, MatchTeam team) {
        List<Integer> scores = new ArrayList<>();

        for (MatchScoreSubmission submission : submissions) {
            if (submission.getTeam1().getId().equals(team.getId())) {
                scores.add(submission.getTeam1Score());
            } else if (submission.getTeam2().getId().equals(team.getId())) {
                scores.add(submission.getTeam2Score());
            }
        }
        Collections.sort(scores);

        int size = scores.size();
        if (size == 0) {
            return 0;
        }

        if (size % 2 == 0) {
            return (scores.get(size / 2 - 1) + scores.get(size / 2)) / 2.0;
        } else {
            return scores.get(size / 2);
        }
    }
}