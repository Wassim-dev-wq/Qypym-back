package org.fivy.matchservice.application.service.schedulers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.domain.entity.Match;
import org.fivy.matchservice.domain.event.email.MatchVerificationCodeEvent;
import org.fivy.matchservice.domain.repository.MatchRepository;
import org.fivy.matchservice.infrastructure.config.kafka.KafkaConfig;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchCodeSchedulerService {

    private final MatchRepository matchRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final int CODE_LENGTH = 6;
    private static final long CODE_VALIDITY_MINUTES = 120;
    private static final long GENERATE_CODE_BEFORE_MINUTES = 60;

    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void generateUpcomingMatchCodes() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Paris"));
        ZonedDateTime upcomingThreshold = now.plus(Duration.ofMinutes(GENERATE_CODE_BEFORE_MINUTES));
        log.info("Checking for upcoming matches that need verification codes");
        List<Match> upcomingMatches = matchRepository.findUpcomingMatchesNeedingCodes(
                now, upcomingThreshold);
        if (upcomingMatches.isEmpty()) {
            log.debug("No upcoming matches need verification codes");
            return;
        }
        log.info("Generating verification codes for {} upcoming matches", upcomingMatches.size());
        for (Match match : upcomingMatches) {
            try {
                generateCodeForMatch(match, now);
            } catch (Exception e) {
                log.error("Error generating verification code for match {}", match.getId(), e);
            }
        }
    }

    @Transactional
    public void generateCodeForMatch(Match match, ZonedDateTime now) {
        String verificationCode = generateRandomCode();
        ZonedDateTime expiryTime = match.getStartDate().plus(Duration.ofMinutes(CODE_VALIDITY_MINUTES));
        match.setVerificationCode(verificationCode);
        match.setCodeExpiryTime(expiryTime);
        Match savedMatch = matchRepository.save(match);
        log.info("Generated verification code for match: {} starting at {}",
                match.getId(), match.getStartDate());
        int ttlMinutes = (int) Duration.between(now, expiryTime).toMinutes();
        MatchVerificationCodeEvent event = MatchVerificationCodeEvent.builder()
                .eventId(UUID.randomUUID())
                .matchId(savedMatch.getId())
                .creatorId(savedMatch.getCreatorId())
                .verificationCode(verificationCode)
                .expiryTime(expiryTime)
                .verificationCodeTtl(ttlMinutes)
                .matchTitle(savedMatch.getTitle())
                .matchLocation(savedMatch.getLocation() != null ? savedMatch.getLocation().getAddress() : "")
                .matchStartDate(savedMatch.getStartDate())
                .matchFormat(savedMatch.getFormat().toString())
                .build();
        event.setEventType("MATCH_VERIFICATION_CODE");
        event.setTimestamp(ZonedDateTime.now());
        log.debug("Publishing verification code event for match: {}", match.getId());
        kafkaTemplate.send(KafkaConfig.TOPIC_MATCH_VERIFICATION, match.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish verification code event for match: {}", match.getId(), ex);
                    } else {
                        log.info("Successfully published verification code event for match: {}", match.getId());
                    }
                });
    }

    private String generateRandomCode() {
        return String.format("%0" + CODE_LENGTH + "d",
                ThreadLocalRandom.current().nextInt(1, (int) Math.pow(10, CODE_LENGTH)));
    }
}