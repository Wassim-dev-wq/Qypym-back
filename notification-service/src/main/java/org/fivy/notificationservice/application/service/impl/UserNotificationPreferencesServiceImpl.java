package org.fivy.notificationservice.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.notificationservice.api.dto.request.UserNotificationPreferencesRequest;
import org.fivy.notificationservice.api.dto.response.UserNotificationPreferencesResponse;
import org.fivy.notificationservice.application.service.UserNotificationPreferencesService;
import org.fivy.notificationservice.domain.entity.UserNotificationPreferences;
import org.fivy.notificationservice.domain.repository.UserNotificationPreferencesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserNotificationPreferencesServiceImpl implements UserNotificationPreferencesService {

    private final UserNotificationPreferencesRepository preferencesRepository;

    @Override
    @Transactional(readOnly = true)
    public UserNotificationPreferencesResponse getPreferences(UUID userId) {
        UserNotificationPreferences preferences = preferencesRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.debug("Creating default notification preferences for user: {}", userId);
                    UserNotificationPreferences newPrefs = UserNotificationPreferences.builder()
                            .userId(userId)
                            .build();
                    return preferencesRepository.save(newPrefs);
                });

        return mapToResponse(preferences);
    }

    @Override
    @Transactional
    public UserNotificationPreferencesResponse updatePreferences(UserNotificationPreferencesRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        UserNotificationPreferences preferences = preferencesRepository.findByUserId(request.getUserId())
                .orElseGet(() -> UserNotificationPreferences.builder()
                        .userId(request.getUserId())
                        .build());

        if (request.getEmailMatchReminders() != null) {
            preferences.setEmailMatchReminders(request.getEmailMatchReminders());
        }

        if (request.getEmailMatchUpdates() != null) {
            preferences.setEmailMatchUpdates(request.getEmailMatchUpdates());
        }

        if (request.getEmailPasswordReset() != null) {
            preferences.setEmailPasswordReset(request.getEmailPasswordReset());
        }

        if (request.getEmailVerification() != null) {
            preferences.setEmailVerification(request.getEmailVerification());
        }

        if (request.getPushMatchJoinRequests() != null) {
            preferences.setPushMatchJoinRequests(request.getPushMatchJoinRequests());
        }

        if (request.getPushMatchInvitations() != null) {
            preferences.setPushMatchInvitations(request.getPushMatchInvitations());
        }

        if (request.getPushMatchUpdates() != null) {
            preferences.setPushMatchUpdates(request.getPushMatchUpdates());
        }

        if (request.getPushChatMessages() != null) {
            preferences.setPushChatMessages(request.getPushChatMessages());
        }

        if (request.getPushTeamUpdates() != null) {
            preferences.setPushTeamUpdates(request.getPushTeamUpdates());
        }

        if (request.getPushMatchReminders() != null) {
            preferences.setPushMatchReminders(request.getPushMatchReminders());
        }

        UserNotificationPreferences saved = preferencesRepository.save(preferences);
        log.info("Updated notification preferences for user: {}", request.getUserId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean shouldSendEmailMatchReminder(UUID userId) {
        return preferencesRepository.findByUserId(userId)
                .map(UserNotificationPreferences::isEmailMatchReminders)
                .orElse(true);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean shouldSendEmailMatchUpdate(UUID userId) {
        return preferencesRepository.findByUserId(userId)
                .map(UserNotificationPreferences::isEmailMatchUpdates)
                .orElse(true);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean shouldSendPushMatchUpdate(UUID userId) {
        return preferencesRepository.findByUserId(userId)
                .map(UserNotificationPreferences::isPushMatchUpdates)
                .orElse(true);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean shouldSendPushChatMessage(UUID userId) {
        return preferencesRepository.findByUserId(userId)
                .map(UserNotificationPreferences::isPushChatMessages)
                .orElse(true);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean shouldSendPushTeamUpdate(UUID userId) {
        return preferencesRepository.findByUserId(userId)
                .map(UserNotificationPreferences::isPushTeamUpdates)
                .orElse(true);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean shouldSendPushMatchJoinRequest(UUID userId) {
        return preferencesRepository.findByUserId(userId)
                .map(UserNotificationPreferences::isPushMatchJoinRequests)
                .orElse(true);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean shouldSendPushMatchInvitation(UUID userId) {
        return preferencesRepository.findByUserId(userId)
                .map(UserNotificationPreferences::isPushMatchInvitations)
                .orElse(true);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean shouldSendPushMatchReminder(UUID userId) {
        return preferencesRepository.findByUserId(userId)
                .map(UserNotificationPreferences::isPushMatchReminders)
                .orElse(true);
    }

    private UserNotificationPreferencesResponse mapToResponse(UserNotificationPreferences entity) {
        return UserNotificationPreferencesResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .emailMatchReminders(entity.isEmailMatchReminders())
                .emailMatchUpdates(entity.isEmailMatchUpdates())
                .emailPasswordReset(entity.isEmailPasswordReset())
                .emailVerification(entity.isEmailVerification())
                .pushMatchJoinRequests(entity.isPushMatchJoinRequests())
                .pushMatchInvitations(entity.isPushMatchInvitations())
                .pushMatchUpdates(entity.isPushMatchUpdates())
                .pushChatMessages(entity.isPushChatMessages())
                .pushTeamUpdates(entity.isPushTeamUpdates())
                .pushMatchReminders(entity.isPushMatchReminders())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}