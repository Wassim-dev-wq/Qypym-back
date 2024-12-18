package org.fivy.userservice.service;

import org.fivy.userservice.dto.UserPreferencesDTO;
import org.fivy.userservice.entity.UserPreferences;

import java.util.UUID;

public interface UserPreferencesService {

    UserPreferencesDTO createUserPreferences(UUID userId, UserPreferencesDTO preferencesDTO);
    UserPreferencesDTO getUserPreferences(UUID userId);

    UserPreferencesDTO updateUserPreferences(UUID userId, UserPreferencesDTO preferencesDTO);
}