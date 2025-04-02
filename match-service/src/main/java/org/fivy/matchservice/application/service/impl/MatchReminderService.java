package org.fivy.matchservice.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.domain.entity.Match;
import org.fivy.matchservice.domain.entity.MatchPlayer;
import org.fivy.matchservice.domain.enums.MatchStatus;
import org.fivy.matchservice.domain.event.email.MatchReminderEvent;
import org.fivy.matchservice.domain.repository.MatchPlayerRepository;
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


@Service
@RequiredArgsConstructor
@Slf4j
public class MatchReminderService {

    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final long REMINDER_TIME_MINUTES = 120;
    private static final long REMINDER_WINDOW_MINUTES = 10;

    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional(readOnly = true)
    public void sendMatchReminders() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Paris"));
        ZonedDateTime startWindow = now.plus(Duration.ofMinutes(REMINDER_TIME_MINUTES - REMINDER_WINDOW_MINUTES/2));
        ZonedDateTime endWindow = now.plus(Duration.ofMinutes(REMINDER_TIME_MINUTES + REMINDER_WINDOW_MINUTES/2));

        log.info("Checking for matches starting soon between {} and {} for sending reminders",
                startWindow, endWindow);
        List<Match> upcomingMatches = matchRepository.findUpcomingMatchesInTimeWindow(
                startWindow, endWindow, MatchStatus.OPEN);

        if (upcomingMatches.isEmpty()) {
            log.debug("No matches found within the reminder window");
            return;
        }

        log.info("Found {} matches starting soon to send reminders for", upcomingMatches.size());

        for (Match match : upcomingMatches) {
            try {
                sendRemindersForMatch(match);
            } catch (Exception e) {
                log.error("Failed to send reminders for match {} ({})", match.getTitle(), match.getId(), e);
            }
        }
    }

    private void sendRemindersForMatch(Match match) {
        log.debug("Sending reminders for match: {} ({})", match.getTitle(), match.getId());
        List<MatchPlayer> players = matchPlayerRepository.findAllByMatchId(match.getId());
        if (players.isEmpty()) {
            log.info("No players to send reminder for match {}", match.getId());
            return;
        }
        for (MatchPlayer player : players) {
            try {
                MatchReminderEvent event = MatchReminderEvent.builder()
                        .eventId(UUID.randomUUID())
                        .matchId(match.getId())
                        .playerId(player.getPlayerId())
                        .teamId(player.getTeam() != null ? player.getTeam().getId() : null)
                        .teamName(player.getTeam() != null ? player.getTeam().getName() : "")
                        .playerRole(player.getRole().toString())
                        .matchTitle(match.getTitle())
                        .matchLocation(match.getLocation() != null ? match.getLocation().getAddress() : "")
                        .matchStartDate(match.getStartDate())
                        .matchFormat(match.getFormat().toString())
                        .skillLevel(match.getSkillLevel().toString())
                        .build();

                event.setEventType("MATCH_REMINDER");
                event.setTimestamp(ZonedDateTime.now());
                kafkaTemplate.send(KafkaConfig.TOPIC_MATCH_VERIFICATION,
                                player.getPlayerId().toString(), event)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to publish reminder event for player: {} in match: {}",
                                        player.getPlayerId(), match.getId(), ex);
                            } else {
                                log.debug("Successfully published reminder event for player: {} in match: {}",
                                        player.getPlayerId(), match.getId());
                            }
                        });
            } catch (Exception e) {
                log.error("Error sending reminder for player {} in match {}",
                        player.getPlayerId(), match.getId(), e);
            }
        }

        log.info("Sent reminders to {} players for match: {} ({})",
                players.size(), match.getTitle(), match.getId());
    }
}