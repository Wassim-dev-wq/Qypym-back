package org.fivy.notificationservice.application.service;

import org.fivy.notificationservice.api.dto.request.UserNotificationPreferencesRequest;
import org.fivy.notificationservice.api.dto.response.UserNotificationPreferencesResponse;

import java.util.UUID;

public interface UserNotificationPreferencesService {

    UserNotificationPreferencesResponse getPreferences(UUID userId);
    UserNotificationPreferencesResponse updatePreferences(UserNotificationPreferencesRequest request);
    boolean shouldSendEmailMatchReminder(UUID userId);
    boolean shouldSendEmailMatchUpdate(UUID userId);
    boolean shouldSendPushMatchUpdate(UUID userId);
    boolean shouldSendPushChatMessage(UUID userId);
    boolean shouldSendPushTeamUpdate(UUID userId);
    boolean shouldSendPushMatchJoinRequest(UUID userId);
    boolean shouldSendPushMatchInvitation(UUID userId);
    boolean shouldSendPushMatchReminder(UUID userId);
}