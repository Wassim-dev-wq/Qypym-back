package org.fivy.notificationservice.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNotificationPreferencesResponse {
    private UUID id;
    private UUID userId;
    private boolean emailMatchReminders;
    private boolean emailMatchUpdates;
    private boolean emailPasswordReset;
    private boolean emailVerification;
    private boolean pushMatchJoinRequests;
    private boolean pushMatchInvitations;
    private boolean pushMatchUpdates;
    private boolean pushChatMessages;
    private boolean pushTeamUpdates;
    private boolean pushMatchReminders;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}