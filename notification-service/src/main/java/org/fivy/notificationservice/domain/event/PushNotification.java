package org.fivy.notificationservice.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fivy.notificationservice.domain.enums.NotificationType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PushNotification {
    private String userId;
    private String matchCreatorId;
    private String title;
    private String body;
    private String matchId;
    private NotificationType type;
    private boolean needsAction;
}
