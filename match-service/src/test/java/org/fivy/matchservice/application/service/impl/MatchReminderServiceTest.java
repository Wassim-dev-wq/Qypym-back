package org.fivy.matchservice.application.service.impl;

import org.fivy.matchservice.domain.entity.Location;
import org.fivy.matchservice.domain.entity.Match;
import org.fivy.matchservice.domain.entity.MatchPlayer;
import org.fivy.matchservice.domain.entity.MatchTeam;
import org.fivy.matchservice.domain.enums.MatchFormat;
import org.fivy.matchservice.domain.enums.MatchStatus;
import org.fivy.matchservice.domain.enums.PlayerRole;
import org.fivy.matchservice.domain.enums.SkillLevel;
import org.fivy.matchservice.domain.event.email.MatchReminderEvent;
import org.fivy.matchservice.domain.repository.MatchPlayerRepository;
import org.fivy.matchservice.domain.repository.MatchRepository;
import org.fivy.matchservice.infrastructure.config.kafka.KafkaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MatchReminderServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private MatchReminderService matchReminderService;

    @Captor
    private ArgumentCaptor<MatchReminderEvent> reminderEventCaptor;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    private Match match1;
    private Match match2;
    private MatchPlayer player1;
    private MatchPlayer player2;
    private MatchPlayer player3;
    private UUID matchId1;
    private UUID matchId2;
    private UUID playerId1;
    private UUID playerId2;
    private UUID playerId3;
    private UUID teamId;
    private MatchTeam team;
    private ZonedDateTime now;
    private ZonedDateTime reminderTime;

    @BeforeEach
    void setUp() {

        now = ZonedDateTime.now();
        reminderTime = now.plus(Duration.ofMinutes(300));


        matchId1 = UUID.randomUUID();
        matchId2 = UUID.randomUUID();
        playerId1 = UUID.randomUUID();
        playerId2 = UUID.randomUUID();
        playerId3 = UUID.randomUUID();
        teamId = UUID.randomUUID();


        Location location = new Location();
        location.setAddress("123 Main St");


        team = new MatchTeam();
        team.setId(teamId);
        team.setName("Team A");


        match1 = new Match();
        match1.setId(matchId1);
        match1.setTitle("Soccer Match");
        match1.setStatus(MatchStatus.OPEN);
        match1.setStartDate(reminderTime);
        match1.setLocation(location);
        match1.setFormat(MatchFormat.FIVE_V_FIVE);
        match1.setSkillLevel(SkillLevel.INTERMEDIATE);


        match2 = new Match();
        match2.setId(matchId2);
        match2.setTitle("Basketball Match");
        match2.setStatus(MatchStatus.OPEN);
        match2.setStartDate(reminderTime.plusMinutes(30));
        match2.setLocation(location);
        match2.setFormat(MatchFormat.EIGHT_V_EIGHT);
        match2.setSkillLevel(SkillLevel.BEGINNER);


        player1 = new MatchPlayer();
        player1.setPlayerId(playerId1);
        player1.setMatch(match1);
        player1.setTeam(team);
        player1.setRole(PlayerRole.FORWARD);

        player2 = new MatchPlayer();
        player2.setPlayerId(playerId2);
        player2.setMatch(match1);
        player2.setTeam(team);
        player2.setRole(PlayerRole.DEFENDER);

        player3 = new MatchPlayer();
        player3.setPlayerId(playerId3);
        player3.setMatch(match2);
        player3.setTeam(null);
        player3.setRole(PlayerRole.GOALKEEPER);


        lenient().when(kafkaTemplate.send(anyString(), anyString(), any(MatchReminderEvent.class)))
                .thenReturn(mock(CompletableFuture.class));
    }

    @Test
    void sendMatchReminders_WithMatchesInTimeWindow() {

        ZonedDateTime startWindow = now.plus(Duration.ofMinutes(295));
        ZonedDateTime endWindow = now.plus(Duration.ofMinutes(305));

        when(matchRepository.findUpcomingMatchesInTimeWindow(
                any(ZonedDateTime.class), any(ZonedDateTime.class), eq(MatchStatus.OPEN)))
                .thenReturn(Arrays.asList(match1, match2));

        when(matchPlayerRepository.findAllByMatchId(matchId1))
                .thenReturn(Arrays.asList(player1, player2));

        when(matchPlayerRepository.findAllByMatchId(matchId2))
                .thenReturn(Collections.singletonList(player3));


        matchReminderService.sendMatchReminders();


        verify(matchRepository).findUpcomingMatchesInTimeWindow(
                any(ZonedDateTime.class), any(ZonedDateTime.class), eq(MatchStatus.OPEN));

        verify(matchPlayerRepository).findAllByMatchId(matchId1);
        verify(matchPlayerRepository).findAllByMatchId(matchId2);


        verify(kafkaTemplate, times(3)).send(
                topicCaptor.capture(),
                keyCaptor.capture(),
                reminderEventCaptor.capture());

        List<String> topics = topicCaptor.getAllValues();
        List<String> keys = keyCaptor.getAllValues();
        List<MatchReminderEvent> events = reminderEventCaptor.getAllValues();


        for (String topic : topics) {
            assertEquals(KafkaConfig.TOPIC_MATCH_VERIFICATION, topic);
        }


        assertTrue(keys.contains(playerId1.toString()));
        assertTrue(keys.contains(playerId2.toString()));
        assertTrue(keys.contains(playerId3.toString()));


        MatchReminderEvent player1Event = events.stream()
                .filter(e -> e.getPlayerId().equals(playerId1))
                .findFirst()
                .orElse(null);

        assertNotNull(player1Event);
        assertEquals(matchId1, player1Event.getMatchId());
        assertEquals("Soccer Match", player1Event.getMatchTitle());
        assertEquals(teamId, player1Event.getTeamId());
        assertEquals("Team A", player1Event.getTeamName());
        assertEquals("FORWARD", player1Event.getPlayerRole());
        assertEquals("123 Main St", player1Event.getMatchLocation());
        assertEquals("FIVE_V_FIVE", player1Event.getMatchFormat());
        assertEquals("INTERMEDIATE", player1Event.getSkillLevel());
        assertEquals("MATCH_REMINDER", player1Event.getEventType());
        assertNotNull(player1Event.getTimestamp());


        MatchReminderEvent player3Event = events.stream()
                .filter(e -> e.getPlayerId().equals(playerId3))
                .findFirst()
                .orElse(null);

        assertNotNull(player3Event);
        assertEquals(matchId2, player3Event.getMatchId());
        assertEquals("Basketball Match", player3Event.getMatchTitle());
        assertNull(player3Event.getTeamId());
        assertEquals("", player3Event.getTeamName());
        assertEquals("GOALKEEPER", player3Event.getPlayerRole());
    }

    @Test
    void sendMatchReminders_NoMatchesInTimeWindow() {

        when(matchRepository.findUpcomingMatchesInTimeWindow(
                any(ZonedDateTime.class), any(ZonedDateTime.class), eq(MatchStatus.OPEN)))
                .thenReturn(Collections.emptyList());


        matchReminderService.sendMatchReminders();


        verify(matchRepository).findUpcomingMatchesInTimeWindow(
                any(ZonedDateTime.class), any(ZonedDateTime.class), eq(MatchStatus.OPEN));


        verifyNoMoreInteractions(matchPlayerRepository);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void sendMatchReminders_NoPlayersInMatch() {

        when(matchRepository.findUpcomingMatchesInTimeWindow(
                any(ZonedDateTime.class), any(ZonedDateTime.class), eq(MatchStatus.OPEN)))
                .thenReturn(Collections.singletonList(match1));

        when(matchPlayerRepository.findAllByMatchId(matchId1))
                .thenReturn(Collections.emptyList());


        matchReminderService.sendMatchReminders();


        verify(matchRepository).findUpcomingMatchesInTimeWindow(
                any(ZonedDateTime.class), any(ZonedDateTime.class), eq(MatchStatus.OPEN));
        verify(matchPlayerRepository).findAllByMatchId(matchId1);


        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void sendMatchReminders_HandleExceptionGracefully() {

        when(matchRepository.findUpcomingMatchesInTimeWindow(
                any(ZonedDateTime.class), any(ZonedDateTime.class), eq(MatchStatus.OPEN)))
                .thenReturn(Arrays.asList(match1, match2));

        when(matchPlayerRepository.findAllByMatchId(matchId1))
                .thenReturn(Arrays.asList(player1, player2));

        when(matchPlayerRepository.findAllByMatchId(matchId2))
                .thenThrow(new RuntimeException("Database error"));


        matchReminderService.sendMatchReminders();


        verify(matchRepository).findUpcomingMatchesInTimeWindow(
                any(ZonedDateTime.class), any(ZonedDateTime.class), eq(MatchStatus.OPEN));

        verify(matchPlayerRepository).findAllByMatchId(matchId1);
        verify(matchPlayerRepository).findAllByMatchId(matchId2);


        verify(kafkaTemplate, times(2)).send(
                eq(KafkaConfig.TOPIC_MATCH_VERIFICATION),
                anyString(),
                any(MatchReminderEvent.class));
    }

    @Test
    void sendMatchReminders_HandleKafkaExceptionGracefully() {

        when(matchRepository.findUpcomingMatchesInTimeWindow(
                any(ZonedDateTime.class), any(ZonedDateTime.class), eq(MatchStatus.OPEN)))
                .thenReturn(Collections.singletonList(match1));

        when(matchPlayerRepository.findAllByMatchId(matchId1))
                .thenReturn(Collections.singletonList(player1));


        when(kafkaTemplate.send(anyString(), anyString(), any(MatchReminderEvent.class)))
                .thenThrow(new RuntimeException("Kafka error"));


        matchReminderService.sendMatchReminders();


        verify(matchRepository).findUpcomingMatchesInTimeWindow(
                any(ZonedDateTime.class), any(ZonedDateTime.class), eq(MatchStatus.OPEN));
        verify(matchPlayerRepository).findAllByMatchId(matchId1);
        verify(kafkaTemplate).send(
                eq(KafkaConfig.TOPIC_MATCH_VERIFICATION),
                eq(playerId1.toString()),
                any(MatchReminderEvent.class));


    }
}