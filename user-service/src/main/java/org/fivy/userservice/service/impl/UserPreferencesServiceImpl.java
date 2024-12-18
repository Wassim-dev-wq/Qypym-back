package org.fivy.userservice.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.fivy.userservice.dto.UserPreferencesDTO;
import org.fivy.userservice.entity.User;
import org.fivy.userservice.entity.UserPreferences;
import org.fivy.userservice.mapper.UserPreferencesMapper;
import org.fivy.userservice.repository.UserPreferencesRepository;
import org.fivy.userservice.repository.UserRepository;
import org.fivy.userservice.service.UserPreferencesService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserPreferencesServiceImpl implements UserPreferencesService {

    private final UserPreferencesRepository preferencesRepository;
    private final UserRepository userRepository;
    private final UserPreferencesMapper preferencesMapper;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public UserPreferencesDTO createUserPreferences(UUID userId, UserPreferencesDTO preferencesDTO) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found with ID: " + userId);
        }
        User userRef = entityManager.getReference(User.class, userId);
        UserPreferences userPreferences = preferencesMapper.toUserPreferences(preferencesDTO);
        userPreferences.setUser(userRef);
        preferencesRepository.save(userPreferences);
        return preferencesDTO;
    }

    @Cacheable(value = "userPreferences", key = "#userId")
    @Override
    public UserPreferencesDTO getUserPreferences(UUID userId) {
        UserPreferences preferences = preferencesRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Preferences not found for user ID: " + userId));
        return preferencesMapper.toUserPreferencesDTO(preferences);
    }

    @CacheEvict(value = "userPreferences", key = "#userId")
    @Transactional
    @Override
    public UserPreferencesDTO updateUserPreferences(UUID userId, UserPreferencesDTO preferencesDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        UserPreferences preferences = preferencesRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        preferencesMapper.updatePreferencesFromDto(preferencesDTO, preferences);

        UserPreferences updatedPreferences = preferencesRepository.save(preferences);
        return preferencesMapper.toUserPreferencesDTO(updatedPreferences);
    }
}