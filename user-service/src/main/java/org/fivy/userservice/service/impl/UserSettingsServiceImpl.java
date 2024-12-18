package org.fivy.userservice.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.fivy.userservice.dto.UserSettingsDto;
import org.fivy.userservice.entity.User;
import org.fivy.userservice.entity.UserSettings;
import org.fivy.userservice.mapper.UserSettingsMapper;
import org.fivy.userservice.repository.UserRepository;
import org.fivy.userservice.repository.UserSettingsRepository;
import org.fivy.userservice.service.UserSettingsService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSettingsServiceImpl implements UserSettingsService {
    private final UserRepository userRepository;
    private final UserSettingsRepository settingsRepository;
    private final UserSettingsMapper userSettingsMapper;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public UserSettingsDto createUserSettings(UUID userId, UserSettingsDto userSettingsDto) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found with ID: " + userId);
        }
        User userRef = entityManager.getReference(User.class, userId);
        UserSettings userSettings = userSettingsMapper.toEntity(userSettingsDto);
        userSettings.setUser(userRef);
        return userSettingsMapper.toDto(settingsRepository.save(userSettings));
    }

    @Cacheable(value = "userSettings", key = "#userId")
    @Override
    public UserSettingsDto getUserSettings(UUID userId) {
        UserSettings userSettings = settingsRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
        return userSettingsMapper.toDto(userSettings);
    }

}
