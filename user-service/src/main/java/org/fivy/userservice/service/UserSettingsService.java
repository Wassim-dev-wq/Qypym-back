package org.fivy.userservice.service;

import org.fivy.userservice.dto.UserSettingsDto;

import java.util.UUID;

public interface UserSettingsService {

    UserSettingsDto createUserSettings(UUID userId, UserSettingsDto userSettingsDto);
    UserSettingsDto getUserSettings(UUID userId);
}
