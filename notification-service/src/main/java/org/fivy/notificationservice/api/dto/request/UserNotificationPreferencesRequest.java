package org.fivy.notificationservice.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNotificationPreferencesRequest {

    private UUID userId;
    private Boolean emailMatchReminders;
    private Boolean emailMatchUpdates;
    private Boolean emailPasswordReset;
    private Boolean emailVerification;
    private Boolean pushMatchJoinRequests;
    private Boolean pushMatchInvitations;
    private Boolean pushMatchUpdates;
    private Boolean pushChatMessages;
    private Boolean pushTeamUpdates;
    private Boolean pushMatchReminders;
}