package org.fivy.userservice.application.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.userservice.api.dto.UserRequestDTO;
import org.fivy.userservice.api.dto.UserResponseDTO;
import org.fivy.userservice.api.dto.update.ProfileResponse;
import org.fivy.userservice.api.dto.update.UpdateProfileRequest;
import org.fivy.userservice.api.mapper.UserMapper;
import org.fivy.userservice.application.service.UserService;
import org.fivy.userservice.domain.entity.User;
import org.fivy.userservice.domain.enums.PlayerLevel;
import org.fivy.userservice.domain.enums.UserStatus;
import org.fivy.userservice.domain.event.userEvent.UserEvent;
import org.fivy.userservice.domain.event.userEvent.UserEventType;
import org.fivy.userservice.domain.repository.UserRepository;
import org.fivy.userservice.infrastructure.config.messaging.UserEventPublisher;
import org.fivy.userservice.shared.exception.UserException;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private static final String USER_CACHE_KEY_PREFIX = "user:";
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserEventPublisher eventPublisher;
    private final KafkaTemplate<String, UserEvent> kafkaTemplate;
    private final RedisTemplate<String, UserResponseDTO> redisTemplate;

    @Transactional
    @Override
    public UserResponseDTO createUserProfile(UserRequestDTO userRequestDTO) {
        String keycloakUserId = userRequestDTO.getKeycloakUserId();
        String email = userRequestDTO.getEmail();
        Optional<User> existingUser = userRepository.findByKeycloakUserId(keycloakUserId);
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("User already exists with keycloakUserId: " + keycloakUserId);
        }
        userRequestDTO.setEmail(email);
        userRequestDTO.setKeycloakUserId(keycloakUserId);
        User user = userMapper.toUser(userRequestDTO);
        User savedUser = userRepository.save(user);
        UserResponseDTO response = userMapper.toUserResponseDTO(savedUser);
        cacheUser(response);
        eventPublisher.publishUserEvent(UserEvent.builder().type(UserEventType.PROFILE_CREATED).userId(savedUser.getId()).keycloakUserId(savedUser.getKeycloakUserId()).data(Map.of("email", savedUser.getEmail(), "playerLevel", savedUser.getPlayerLevel().toString())).build());

        return response;
    }

    @Transactional
    public UserResponseDTO createUserFromAuthEvent(User userFromEvent) {
        Optional<User> existingUser = userRepository.findByKeycloakUserId(userFromEvent.getKeycloakUserId());
        if (existingUser.isPresent()) {
            log.warn("User already exists with keycloakUserId: {}", userFromEvent.getKeycloakUserId());
            return userMapper.toUserResponseDTO(existingUser.get());
        }
        userFromEvent.setPlayerLevel(PlayerLevel.BEGINNER);
        userFromEvent.setStatus(UserStatus.ACTIVE);
        User savedUser = userRepository.save(userFromEvent);
        UserResponseDTO response = userMapper.toUserResponseDTO(savedUser);
        cacheUser(response);
        eventPublisher.publishUserEvent(UserEvent.builder().type(UserEventType.PROFILE_CREATED).userId(savedUser.getId()).keycloakUserId(savedUser.getKeycloakUserId()).data(Map.of("email", savedUser.getEmail(), "playerLevel", savedUser.getPlayerLevel().toString())).build());
        log.info("Created new user profile from auth event for keycloakUserId: {}", userFromEvent.getKeycloakUserId());
        return response;
    }

    @Override
    public UserResponseDTO getUserById(UUID userId) {
        String cacheKey = USER_CACHE_KEY_PREFIX + userId;
        UserResponseDTO cachedUser = redisTemplate.opsForValue().get(cacheKey);
        if (cachedUser != null) {
            log.debug("Cache hit for user: {}", userId);
            return cachedUser;
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
        UserResponseDTO response = userMapper.toUserResponseDTO(user);
        cacheUser(response);

        return response;
    }

    @Transactional
    @Override
    @CachePut(value = "users", key = "#userId")
    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        log.info("Updating profile for userId: {}", userId);

        User existingUser = userRepository.findById(userId).orElseThrow(() -> new UserException("User not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        try {
            userMapper.updateUserFromDto(request, existingUser);
            existingUser.setUpdatedAt(Instant.from(LocalDateTime.now()));
            User updatedUser = userRepository.save(existingUser);
            ProfileResponse response = userMapper.toProfileResponse(updatedUser);
            publishProfileUpdatedEvent(updatedUser);

            log.info("Successfully updated profile for userId: {}", userId);
            return response;

        } catch (Exception e) {
            log.error("Failed to update profile for userId: {}", userId, e);
            throw new UserException("Failed to update profile", "UPDATE_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    private void publishProfileUpdatedEvent(User user) {
        UserEvent event = UserEvent.builder().type(UserEventType.PROFILE_UPDATED).userId(user.getId()).keycloakUserId(user.getKeycloakUserId()).data(Map.of("email", user.getEmail(), "username", user.getUsername(), "playerLevel", String.valueOf(user.getPlayerLevel()))).timestamp(Instant.from(LocalDateTime.now())).build();

        kafkaTemplate.send("user-events", String.valueOf(user.getId()), event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish profile updated event for userId: {}", user.getId(), ex);
            }
        });
    }

    @Transactional
    @Override
    public void deleteUser(UUID userId) {
        userRepository.updateUserStatus(userId, UserStatus.DELETED);
        redisTemplate.delete(USER_CACHE_KEY_PREFIX + userId);
        eventPublisher.publishUserEvent(UserEvent.builder().type(UserEventType.USER_DELETED).userId(userId).build());
    }

    private void cacheUser(UserResponseDTO user) {
        String cacheKey = USER_CACHE_KEY_PREFIX + user.getId();
        redisTemplate.opsForValue().set(cacheKey, user, Duration.ofHours(24));
    }
}