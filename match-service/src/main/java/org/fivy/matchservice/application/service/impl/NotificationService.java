package org.fivy.matchservice.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.domain.entity.Match;
import org.fivy.matchservice.domain.entity.MatchPlayer;
import org.fivy.matchservice.domain.enums.NotificationType;
import org.fivy.matchservice.domain.event.MatchEventType;
import org.fivy.matchservice.domain.event.PushNotification;
import org.fivy.matchservice.domain.repository.MatchPlayerRepository;
import org.fivy.matchservice.infrastructure.config.kafka.KafkaConfig;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm", Locale.FRENCH);
    private final KafkaTemplate<String, PushNotification> notificationKafkaTemplate;
    private final MatchPlayerRepository playerRepository;

    public void sendMatchEventNotifications(Match match, MatchEventType eventType) {
        log.debug("Preparing notifications for event: {} for match: {}", eventType, match.getId());

        List<PushNotification> notifications = createNotifications(match, eventType);

        if (notifications.isEmpty()) {
            log.debug("No notifications to send for this event");
            return;
        }

        for (PushNotification notification : notifications) {
            notificationKafkaTemplate.send(
                    KafkaConfig.TOPIC_PUSH_NOTIFICATIONS,
                    notification.getUserId(),
                    notification
            ).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send notification of type: {}", notification.getType(), ex);
                } else {
                    log.debug("Notification of type: {} sent successfully to user: {}",
                            notification.getType(), notification.getUserId());
                }
            });
        }
    }

    private List<PushNotification> createNotifications(Match match, MatchEventType eventType) {
        List<PushNotification> notifications = new ArrayList<>();

        switch (eventType) {
            case MATCH_CREATED:
                break;

            case MATCH_UPDATED:
                notifications.addAll(createMatchUpdatedNotifications(match));
                break;

            case MATCH_STATUS_UPDATED:
                notifications.addAll(createStatusChangeNotifications(match));
                break;

            case MATCH_DELETED:
                notifications.addAll(createMatchDeletedNotifications(match));
                break;

            case PLAYER_JOINED:
                notifications.addAll(createPlayerJoinedNotifications(match,
                        UUID.fromString(match.getLastEventData().get("playerId"))));
                break;

            case PLAYER_LEFT:
                notifications.addAll(createPlayerLeftNotifications(match,
                        UUID.fromString(match.getLastEventData().get("playerId"))));
                break;

            default:
                log.debug("Type d'événement sans notification associée: {}", eventType);
        }

        return notifications;
    }

    private List<PushNotification> createStatusChangeNotifications(Match match) {
        List<UUID> playerIds = getMatchPlayerIds(match);
        List<PushNotification> notifications = new ArrayList<>();
        String title;
        String body;
        NotificationType type = NotificationType.MATCH_STATUS_CHANGED;
        switch (match.getStatus()) {
            case OPEN:
                title = "Match ouvert aux inscriptions";
                body = String.format("Le match \"%s\" est maintenant ouvert aux inscriptions pour le %s.",
                        match.getTitle(), match.getStartDate().format(DATE_FORMATTER));
                break;

            case IN_PROGRESS:
                title = "Match en cours";
                body = String.format("Le match \"%s\" a commencé ! Rendez-vous à %s.",
                        match.getTitle(), match.getLocation().getAddress());
                break;

            case COMPLETED:
                title = "Match terminé";
                body = String.format("Le match \"%s\" est maintenant terminé. Merci pour votre participation !",
                        match.getTitle());
                break;

            case CANCELLED:
                title = "Match annulé";
                body = String.format("Le match \"%s\" prévu pour le %s a été annulé.",
                        match.getTitle(), match.getStartDate().format(DATE_FORMATTER));
                break;

            default:
                return notifications;
        }

        for (UUID playerId : playerIds) {
            notifications.add(PushNotification.builder()
                    .userId(playerId.toString())
                    .title(title)
                    .body(body)
                    .matchId(match.getId().toString())
                    .matchCreatorId(match.getCreatorId().toString())
                    .type(type)
                    .needsAction(false)
                    .build());
        }

        return notifications;
    }

    private List<PushNotification> createMatchUpdatedNotifications(Match match) {
        List<UUID> playerIds = getMatchPlayerIds(match);
        List<PushNotification> notifications = new ArrayList<>();

        String title = "Mise à jour du match";
        String body = String.format("Le match \"%s\" a été mis à jour. Vérifiez les nouveaux détails.", match.getTitle());

        for (UUID playerId : playerIds) {
            notifications.add(PushNotification.builder()
                    .userId(playerId.toString())
                    .title(title)
                    .body(body)
                    .matchId(match.getId().toString())
                    .matchCreatorId(match.getCreatorId().toString())
                    .type(NotificationType.MATCH_UPDATED)
                    .needsAction(false)
                    .build());
        }

        return notifications;
    }

    private List<PushNotification> createMatchDeletedNotifications(Match match) {
        List<UUID> playerIds = getMatchPlayerIds(match);
        List<PushNotification> notifications = new ArrayList<>();

        String title = "Match supprimé";
        String body = String.format("Le match \"%s\" prévu pour le %s a été supprimé.",
                match.getTitle(), match.getStartDate().format(DATE_FORMATTER));

        for (UUID playerId : playerIds) {
            notifications.add(PushNotification.builder()
                    .userId(playerId.toString())
                    .title(title)
                    .body(body)
                    .matchId(match.getId().toString())
                    .matchCreatorId(match.getCreatorId().toString())
                    .type(NotificationType.MATCH_DELETED)
                    .needsAction(false)
                    .build());
        }

        return notifications;
    }

    private List<PushNotification> createPlayerJoinedNotifications(Match match, UUID joinedPlayerId) {
        List<PushNotification> notifications = new ArrayList<>();
        match.getPlayers().stream()
                .filter(player -> !player.getPlayerId().equals(joinedPlayerId))
                .filter(player -> !player.getPlayerId().equals(match.getCreatorId()))
                .map(player -> PushNotification.builder()
                        .userId(player.getPlayerId().toString())
                        .title("Nouveau joueur dans le match")
                        .body(String.format("Un nouveau joueur a rejoint le match \"%s\".", match.getTitle()))
                        .matchId(match.getId().toString())
                        .matchCreatorId(match.getCreatorId().toString())
                        .type(NotificationType.PLAYER_JOINED)
                        .needsAction(false)
                        .build())
                .forEach(notifications::add);
        return notifications;
    }

    private List<PushNotification> createPlayerLeftNotifications(Match match, UUID leftPlayerId) {
        List<PushNotification> notifications = new ArrayList<>();
        if (!match.getCreatorId().equals(leftPlayerId)) {
            String playerName = match.getLastEventData().getOrDefault("playerName", "Un joueur");

            notifications.add(PushNotification.builder()
                    .userId(match.getCreatorId().toString())
                    .title("Un joueur a quitté votre match")
                    .body(String.format("%s a quitté votre match \"%s\".", playerName, match.getTitle()))
                    .matchId(match.getId().toString())
                    .matchCreatorId(match.getCreatorId().toString())
                    .type(NotificationType.PLAYER_LEFT)
                    .needsAction(false)
                    .build());
        }
        return notifications;
    }

    public void sendJoinRequestNotification(Match match, String requesterName) {
        PushNotification notification = PushNotification.builder()
                .userId(match.getCreatorId().toString())
                .title("Nouvelle demande de participation")
                .body(String.format("%s souhaite rejoindre votre match \"%s\".", requesterName, match.getTitle()))
                .matchId(match.getId().toString())
                .matchCreatorId(match.getCreatorId().toString())
                .type(NotificationType.JOIN_REQUEST_RECEIVED)
                .needsAction(true)
                .build();

        notificationKafkaTemplate.send(KafkaConfig.TOPIC_PUSH_NOTIFICATIONS, notification.getUserId(), notification);
        log.debug("Notification de demande de participation envoyée au créateur: {}", match.getCreatorId());
    }

    public void sendJoinRequestResponseNotification(Match match, UUID requesterId, boolean accepted) {
        String title = accepted ? "Demande acceptée" : "Demande refusée";
        String body = accepted ?
                String.format("Votre demande pour rejoindre le match \"%s\" a été acceptée.", match.getTitle()) :
                String.format("Votre demande pour rejoindre le match \"%s\" a été refusée.", match.getTitle());
        NotificationType type = accepted ?
                NotificationType.JOIN_REQUEST_ACCEPTED :
                NotificationType.JOIN_REQUEST_REJECTED;
        PushNotification notification = PushNotification.builder()
                .userId(requesterId.toString())
                .title(title)
                .body(body)
                .matchId(match.getId().toString())
                .matchCreatorId(match.getCreatorId().toString())
                .type(type)
                .needsAction(false)
                .build();
        notificationKafkaTemplate.send(KafkaConfig.TOPIC_PUSH_NOTIFICATIONS, notification.getUserId(), notification);
        log.debug("Notification de réponse à la demande envoyée au demandeur: {}", requesterId);
    }

    protected List<UUID> getMatchPlayerIds(Match match) {
        if (match.getPlayers() != null && !match.getPlayers().isEmpty()) {
            return match.getPlayers().stream()
                    .map(MatchPlayer::getPlayerId)
                    .collect(Collectors.toList());
        }
        return playerRepository.findPlayerIdsByMatchId(match.getId());
    }
}