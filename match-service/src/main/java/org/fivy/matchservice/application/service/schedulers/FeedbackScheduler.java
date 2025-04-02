package org.fivy.matchservice.application.service.schedulers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.application.service.FeedbackService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackScheduler {

    private final FeedbackService feedbackService;

    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void processExpiredFeedbackRequests() {
        log.info("Running scheduled task to process expired feedback requests");
        feedbackService.processExpiredFeedbackRequests();
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void updateAllPlayerRatingSummaries() {
        log.info("Running scheduled task to update all player rating summaries");
        feedbackService.updatePlayerRatingSummaries(null);
    }
}