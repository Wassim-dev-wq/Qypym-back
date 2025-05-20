package org.fivy.matchservice.application.service.impl;

import org.fivy.matchservice.domain.entity.Location;
import org.fivy.matchservice.domain.entity.Match;
import org.fivy.matchservice.domain.entity.MatchPlayer;
import org.fivy.matchservice.domain.enums.MatchStatus;
import org.fivy.matchservice.domain.enums.NotificationType;
import org.fivy.matchservice.domain.enums.PlayerRole;
import org.fivy.matchservice.domain.event.MatchEventType;
import org.fivy.matchservice.domain.event.PushNotification;
import org.fivy.matchservice.domain.repository.MatchPlayerRepository;
import org.fivy.matchservice.infrastructure.config.kafka.KafkaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private KafkaTemplate<String, PushNotification> notificationKafkaTemplate;

    @Mock
    private MatchPlayerRepository playerRepository;

    @InjectMocks
    private NotificationService notificationService;

    private UUID matchId;
    private UUID creatorId;
    private UUID player1Id;
    private UUID player2Id;
    private Match match;
    private MatchPlayer player1;
    private MatchPlayer player2;
    private DateTimeFormatter dateFormatter;

    @BeforeEach
    void setUp() {
        matchId = UUID.randomUUID();
        creatorId = UUID.randomUUID();
        player1Id = UUID.randomUUID();
        player2Id = UUID.randomUUID();
        dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm", Locale.FRENCH);


        match = new Match();
        match.setId(matchId);
        match.setCreatorId(creatorId);
        match.setTitle("Football Match");
        match.setStatus(MatchStatus.OPEN);
        match.setStartDate(ZonedDateTime.now().plusDays(1));

        Location location = new Location();
        location.setAddress("123 Sports St");
        match.setLocation(location);


        player1 = new MatchPlayer();
        player1.setId(UUID.randomUUID());
        player1.setPlayerId(player1Id);
        player1.setMatch(match);
        player1.setRole(PlayerRole.FORWARD);

        player2 = new MatchPlayer();
        player2.setId(UUID.randomUUID());
        player2.setPlayerId(player2Id);
        player2.setMatch(match);
        player2.setRole(PlayerRole.MIDFIELDER);

        Set<MatchPlayer> players = new HashSet<>(Arrays.asList(player1, player2));
        match.setPlayers(players);


        Map<String, String> lastEventData = new HashMap<>();
        lastEventData.put("playerId", player1Id.toString());
        lastEventData.put("playerName", "Un joueur");
        match.setLastEventData(lastEventData);


        lenient().when(notificationKafkaTemplate.send(anyString(), anyString(), any(PushNotification.class)))
                .thenReturn(mock(CompletableFuture.class));
    }

    @Test
    void sendMatchEventNotifications_MatchUpdated() {

        MatchEventType eventType = MatchEventType.MATCH_UPDATED;


        notificationService.sendMatchEventNotifications(match, eventType);


        ArgumentCaptor<PushNotification> notificationCaptor = ArgumentCaptor.forClass(PushNotification.class);
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        verify(notificationKafkaTemplate, times(2)).send(
                topicCaptor.capture(),
                keyCaptor.capture(),
                notificationCaptor.capture()
        );


        List<String> topics = topicCaptor.getAllValues();
        List<PushNotification> notifications = notificationCaptor.getAllValues();
        List<String> keys = keyCaptor.getAllValues();

        for (String topic : topics) {
            assertEquals(KafkaConfig.TOPIC_PUSH_NOTIFICATIONS, topic);
        }

        for (PushNotification notification : notifications) {
            assertEquals(NotificationType.MATCH_UPDATED, notification.getType());
            assertEquals(matchId.toString(), notification.getMatchId());
            assertEquals(creatorId.toString(), notification.getMatchCreatorId());
            assertTrue(notification.getTitle().contains("Mise à jour"));
            assertTrue(notification.getBody().contains(match.getTitle()));
        }


        assertTrue(keys.contains(player1Id.toString()));
        assertTrue(keys.contains(player2Id.toString()));
    }

    @Test
    void sendMatchEventNotifications_MatchStatusUpdated_InProgress() {

        MatchEventType eventType = MatchEventType.MATCH_STATUS_UPDATED;
        match.setStatus(MatchStatus.IN_PROGRESS);


        notificationService.sendMatchEventNotifications(match, eventType);


        ArgumentCaptor<PushNotification> notificationCaptor = ArgumentCaptor.forClass(PushNotification.class);
        verify(notificationKafkaTemplate, times(2)).send(
                eq(KafkaConfig.TOPIC_PUSH_NOTIFICATIONS),
                anyString(),
                notificationCaptor.capture()
        );

        List<PushNotification> notifications = notificationCaptor.getAllValues();
        for (PushNotification notification : notifications) {
            assertEquals(NotificationType.MATCH_STATUS_CHANGED, notification.getType());
            assertTrue(notification.getTitle().contains("Match en cours"));
            assertTrue(notification.getBody().contains(match.getLocation().getAddress()));
        }
    }

    @Test
    void sendMatchEventNotifications_MatchStatusUpdated_Completed() {

        MatchEventType eventType = MatchEventType.MATCH_STATUS_UPDATED;
        match.setStatus(MatchStatus.COMPLETED);


        notificationService.sendMatchEventNotifications(match, eventType);


        ArgumentCaptor<PushNotification> notificationCaptor = ArgumentCaptor.forClass(PushNotification.class);
        verify(notificationKafkaTemplate, times(2)).send(
                eq(KafkaConfig.TOPIC_PUSH_NOTIFICATIONS),
                anyString(),
                notificationCaptor.capture()
        );

        List<PushNotification> notifications = notificationCaptor.getAllValues();
        for (PushNotification notification : notifications) {
            assertEquals(NotificationType.MATCH_STATUS_CHANGED, notification.getType());
            assertTrue(notification.getTitle().contains("Match terminé"));
            assertTrue(notification.getBody().contains("terminé"));
        }
    }

    @Test
    void sendMatchEventNotifications_MatchStatusUpdated_Cancelled() {

        MatchEventType eventType = MatchEventType.MATCH_STATUS_UPDATED;
        match.setStatus(MatchStatus.CANCELLED);


        notificationService.sendMatchEventNotifications(match, eventType);


        ArgumentCaptor<PushNotification> notificationCaptor = ArgumentCaptor.forClass(PushNotification.class);
        verify(notificationKafkaTemplate, times(2)).send(
                eq(KafkaConfig.TOPIC_PUSH_NOTIFICATIONS),
                anyString(),
                notificationCaptor.capture()
        );

        List<PushNotification> notifications = notificationCaptor.getAllValues();
        for (PushNotification notification : notifications) {
            assertEquals(NotificationType.MATCH_STATUS_CHANGED, notification.getType());
            assertTrue(notification.getTitle().contains("annulé"));
            assertTrue(notification.getBody().contains("annulé"));
        }
    }

    @Test
    void sendMatchEventNotifications_MatchDeleted() {

        MatchEventType eventType = MatchEventType.MATCH_DELETED;


        notificationService.sendMatchEventNotifications(match, eventType);


        ArgumentCaptor<PushNotification> notificationCaptor = ArgumentCaptor.forClass(PushNotification.class);
        verify(notificationKafkaTemplate, times(2)).send(
                eq(KafkaConfig.TOPIC_PUSH_NOTIFICATIONS),
                anyString(),
                notificationCaptor.capture()
        );

        List<PushNotification> notifications = notificationCaptor.getAllValues();
        for (PushNotification notification : notifications) {
            assertEquals(NotificationType.MATCH_DELETED, notification.getType());
            assertTrue(notification.getTitle().contains("supprimé"));
            assertTrue(notification.getBody().contains("supprimé"));
        }
    }

    @Test
    void sendMatchEventNotifications_PlayerJoined() {

        MatchEventType eventType = MatchEventType.PLAYER_JOINED;


        Map<String, String> lastEventData = new HashMap<>();
        lastEventData.put("playerId", player1Id.toString());
        lastEventData.put("playerName", "Un joueur");
        match.setLastEventData(lastEventData);


        notificationService.sendMatchEventNotifications(match, eventType);



        verify(notificationKafkaTemplate, times(1)).send(
                eq(KafkaConfig.TOPIC_PUSH_NOTIFICATIONS),
                eq(player2Id.toString()),
                any(PushNotification.class)
        );
    }

    @Test
    void sendMatchEventNotifications_PlayerLeft() {

        MatchEventType eventType = MatchEventType.PLAYER_LEFT;


        notificationService.sendMatchEventNotifications(match, eventType);



        ArgumentCaptor<PushNotification> notificationCaptor = ArgumentCaptor.forClass(PushNotification.class);
        verify(notificationKafkaTemplate).send(
                eq(KafkaConfig.TOPIC_PUSH_NOTIFICATIONS),
                eq(creatorId.toString()),
                notificationCaptor.capture()
        );

        PushNotification notification = notificationCaptor.getValue();
        assertEquals(NotificationType.PLAYER_LEFT, notification.getType());
        assertTrue(notification.getTitle().contains("quitté"));
        assertTrue(notification.getBody().contains("quitté"));
    }

    @Test
    void sendMatchEventNotifications_NoNotificationsForCreated() {

        MatchEventType eventType = MatchEventType.MATCH_CREATED;


        notificationService.sendMatchEventNotifications(match, eventType);


        verify(notificationKafkaTemplate, never()).send(
                anyString(),
                anyString(),
                any(PushNotification.class)
        );
    }

    @Test
    void sendJoinRequestNotification_Success() {

        String requesterName = "Un joueur";


        notificationService.sendJoinRequestNotification(match, requesterName);


        ArgumentCaptor<PushNotification> notificationCaptor = ArgumentCaptor.forClass(PushNotification.class);
        verify(notificationKafkaTemplate).send(
                eq(KafkaConfig.TOPIC_PUSH_NOTIFICATIONS),
                eq(creatorId.toString()),
                notificationCaptor.capture()
        );

        PushNotification notification = notificationCaptor.getValue();
        assertEquals(NotificationType.JOIN_REQUEST_RECEIVED, notification.getType());
        assertTrue(notification.getTitle().contains("Nouvelle demande"));
        assertTrue(notification.getBody().contains(requesterName));
        assertTrue(notification.isNeedsAction());
    }

    @Test
    void sendJoinRequestResponseNotification_Accepted() {

        notificationService.sendJoinRequestResponseNotification(match, player1Id, true);


        ArgumentCaptor<PushNotification> notificationCaptor = ArgumentCaptor.forClass(PushNotification.class);
        verify(notificationKafkaTemplate).send(
                eq(KafkaConfig.TOPIC_PUSH_NOTIFICATIONS),
                eq(player1Id.toString()),
                notificationCaptor.capture()
        );

        PushNotification notification = notificationCaptor.getValue();
        assertEquals(NotificationType.JOIN_REQUEST_ACCEPTED, notification.getType());
        assertTrue(notification.getTitle().contains("acceptée"));
        assertTrue(notification.getBody().contains("acceptée"));
        assertFalse(notification.isNeedsAction());
    }

    @Test
    void sendJoinRequestResponseNotification_Rejected() {

        notificationService.sendJoinRequestResponseNotification(match, player1Id, false);


        ArgumentCaptor<PushNotification> notificationCaptor = ArgumentCaptor.forClass(PushNotification.class);
        verify(notificationKafkaTemplate).send(
                eq(KafkaConfig.TOPIC_PUSH_NOTIFICATIONS),
                eq(player1Id.toString()),
                notificationCaptor.capture()
        );

        PushNotification notification = notificationCaptor.getValue();
        assertEquals(NotificationType.JOIN_REQUEST_REJECTED, notification.getType());
        assertTrue(notification.getTitle().contains("refusée"));
        assertTrue(notification.getBody().contains("refusée"));
        assertFalse(notification.isNeedsAction());
    }

    @Test
    void getMatchPlayerIds_FromMatch() {

        List<UUID> playerIds = notificationService.getMatchPlayerIds(match);


        assertEquals(2, playerIds.size());
        assertTrue(playerIds.contains(player1Id));
        assertTrue(playerIds.contains(player2Id));
    }

    @Test
    void getMatchPlayerIds_FromRepository() {

        match.setPlayers(null);
        when(playerRepository.findPlayerIdsByMatchId(matchId))
                .thenReturn(Arrays.asList(player1Id, player2Id));


        List<UUID> playerIds = notificationService.getMatchPlayerIds(match);


        assertEquals(2, playerIds.size());
        assertTrue(playerIds.contains(player1Id));
        assertTrue(playerIds.contains(player2Id));
        verify(playerRepository).findPlayerIdsByMatchId(matchId);
    }

    @Test
    void getMatchPlayerIds_EmptyPlayers() {

        match.setPlayers(Collections.emptySet());
        when(playerRepository.findPlayerIdsByMatchId(matchId))
                .thenReturn(Arrays.asList(player1Id, player2Id));


        List<UUID> playerIds = notificationService.getMatchPlayerIds(match);


        assertEquals(2, playerIds.size());
        verify(playerRepository).findPlayerIdsByMatchId(matchId);
    }

    @Test
    void createNotifications_MatchUpdated() throws Exception {

        Method method = NotificationService.class.getDeclaredMethod("createNotifications", Match.class, MatchEventType.class);
        method.setAccessible(true);


        @SuppressWarnings("unchecked")
        List<PushNotification> notifications = (List<PushNotification>) method.invoke(notificationService, match, MatchEventType.MATCH_UPDATED);


        assertEquals(2, notifications.size());
        for (PushNotification notification : notifications) {
            assertEquals(NotificationType.MATCH_UPDATED, notification.getType());
            assertTrue(notification.getTitle().contains("Mise à jour"));
        }
    }

    @Test
    void createNotifications_NoNotificationsForUnhandledType() throws Exception {

        Method method = NotificationService.class.getDeclaredMethod("createNotifications", Match.class, MatchEventType.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<PushNotification> notifications = (List<PushNotification>) method.invoke(
                notificationService, match, MatchEventType.MATCH_CREATED);

        assertTrue(notifications.isEmpty());
    }
}