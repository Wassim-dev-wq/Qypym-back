package org.fivy.matchservice.domain.event;

import lombok.Builder;
import lombok.Data;
import org.fivy.matchservice.domain.enums.NotificationType;

@Data
@Builder
public class PushNotification {
    private String userId;
    private String matchCreatorId;
    private String title;
    private String body;
    private String matchId;
    private NotificationType type;
    private boolean needsAction;
}
