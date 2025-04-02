package org.fivy.matchservice.application.service.schedulers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.application.service.FeedbackService;
import org.fivy.matchservice.domain.entity.Match;
import org.fivy.matchservice.domain.enums.MatchStatus;
import org.fivy.matchservice.domain.repository.MatchRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchFinishScheduler {

    private static final long CHECK_INTERVAL = 60 * 1000;

    private final MatchRepository matchRepository;
    private final FeedbackService feedbackService;

    @Scheduled(fixedRate = CHECK_INTERVAL)
    public void finishMatches() {
        log.info("Checking for matches to finish");
        List<Match> matchesToFinish = matchRepository.findMatchesToFinish();
        if (matchesToFinish.isEmpty()) {
            log.debug("No matches to finish");
            return;
        }
        log.info("Finishing {} matches", matchesToFinish.size());
        for (Match match : matchesToFinish) {
            try {
                finishMatch(match);
            } catch (Exception e) {
                log.error("Error finishing match {}", match.getId(), e);
            }
        }
    }

    private void finishMatch(Match match) {
        match.setStatus(MatchStatus.FINISHED);
        matchRepository.save(match);
        try {
            feedbackService.createFeedbackRequest(match);
            log.info("Created feedback request for match {}", match.getId());
        } catch (Exception e) {
            log.error("Error creating feedback request for match {}", match.getId(), e);
        }
    }
}