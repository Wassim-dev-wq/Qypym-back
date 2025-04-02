package org.fivy.notificationservice.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fivy.notificationservice.domain.enums.NotificationType;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponse {
    private UUID id;
    private UUID userId;
    private UUID matchId;
    private UUID matchCreatorId;
    private NotificationType type;
    private String title;
    private String message;
    private boolean read;
    private ZonedDateTime createdAt;
}
