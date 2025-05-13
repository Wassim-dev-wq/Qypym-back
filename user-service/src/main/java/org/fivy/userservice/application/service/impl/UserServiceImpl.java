package org.fivy.userservice.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.userservice.api.dto.ContentReportDTO;
import org.fivy.userservice.api.dto.UserRequestDTO;
import org.fivy.userservice.api.dto.UserResponseDTO;
import org.fivy.userservice.api.dto.update.UpdateProfileRequest;
import org.fivy.userservice.api.mapper.ContentReportMapper;
import org.fivy.userservice.api.mapper.UserMapper;
import org.fivy.userservice.application.service.UserService;
import org.fivy.userservice.domain.entity.ContentReport;
import org.fivy.userservice.domain.entity.User;
import org.fivy.userservice.domain.entity.UserBlock;
import org.fivy.userservice.domain.enums.PlayerLevel;
import org.fivy.userservice.domain.enums.ReportStatus;
import org.fivy.userservice.domain.enums.UserStatus;
import org.fivy.userservice.domain.event.userEvent.UserEvent;
import org.fivy.userservice.domain.event.userEvent.UserEventType;
import org.fivy.userservice.domain.repository.ContentReportRepository;
import org.fivy.userservice.domain.repository.UserBlockRepository;
import org.fivy.userservice.domain.repository.UserRepository;
import org.fivy.userservice.infrastructure.config.messaging.UserEventPublisher;
import org.fivy.userservice.shared.exception.ProfileModerationService;
import org.fivy.userservice.shared.exception.UserException;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {
    private static final String USER_CACHE_KEY_PREFIX = "user:";
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserEventPublisher eventPublisher;
    private final UserBlockRepository userBlockRepository;
    private final ContentReportRepository contentReportRepository;
    private final ContentReportMapper contentReportMapper;
    private final RedisTemplate<String, UserResponseDTO> redisTemplate;
    private final ProfileModerationService profileModerationService;

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
    @Transactional(readOnly = true)
    @CachePut(value = "users", key = "#userId")
    public UserResponseDTO getUserById(UUID userId) {
        String cacheKey = USER_CACHE_KEY_PREFIX + userId;
        UserResponseDTO cachedUser = redisTemplate.opsForValue().get(cacheKey);
        if (cachedUser != null) {
            log.debug("Cache hit for user: {}", userId);
            return cachedUser;
        }
        User user = userRepository.findByKeycloakUserId(String.valueOf(userId))
                .orElseThrow(() -> new UserException(
                        "User not found",
                        "USER_NOT_FOUND",
                        HttpStatus.NOT_FOUND
                ));
        UserResponseDTO response = userMapper.toUserResponseDTO(user);
        cacheUser(response);
        return response;
    }

    @Override
    @CachePut(value = "users", key = "#userId")
    public UserResponseDTO updateProfile(UUID userId, UpdateProfileRequest request) {
        log.info("Updating profile for userId: {}", userId);
        User existingUser = userRepository.findByKeycloakUserId(String.valueOf(userId))
                .orElseThrow(() -> new UserException("User not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        try {
            profileModerationService.validateProfileContent(request);
            userMapper.updateUserFromDto(request, existingUser);
            existingUser.preUpdate();
            User updatedUser = userRepository.save(existingUser);
            UserResponseDTO response = userMapper.toUserResponseDTO(updatedUser);
            cacheUser(response);
            log.info("Successfully updated profile for userId: {}", userId);
            return response;
        } catch (UserException e) {
            log.warn("Profile update rejected due to moderation rules: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            // Log any other unexpected errors
            log.error("Failed to update profile for userId: {}", userId, e);
            throw new UserException("Failed to update profile", "UPDATE_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @Override
    public void deleteUser(UUID userId) {
        userRepository.updateUserStatus(userId, UserStatus.DELETED);
        redisTemplate.delete(USER_CACHE_KEY_PREFIX + userId);
        eventPublisher.publishUserEvent(UserEvent.builder().type(UserEventType.USER_DELETED).userId(userId).build());
    }

    @Override
    public UserResponseDTO uploadProfilePhoto(UUID userId, MultipartFile file) throws IOException {
        log.info("Uploading profile photo for userId: {}", userId);
        User user = userRepository.findByKeycloakUserId(String.valueOf(userId))
                .orElseThrow(() -> new UserException("User not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        try {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new UserException("Invalid file type. Only images are allowed.",
                        "INVALID_FILE_TYPE", HttpStatus.BAD_REQUEST);
            }
            if (file.getSize() > 5 * 1024 * 1024) {
                throw new UserException("File too large. Maximum size is 5MB.",
                        "FILE_TOO_LARGE", HttpStatus.BAD_REQUEST);
            }
            user.setProfilePhoto(file.getBytes());
            user.setPhotoContentType(contentType);
            User updatedUser = userRepository.save(user);
            UserResponseDTO response = userMapper.toUserResponseDTO(updatedUser);
            cacheUser(response);

            log.info("Successfully uploaded profile photo for userId: {}", userId);
            return response;
        } catch (IOException e) {
            log.error("Failed to upload profile photo for userId: {}", userId, e);
            throw new UserException("Failed to upload profile photo", "UPLOAD_FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getProfilePhoto(UUID userId) {
        log.debug("Getting profile photo for userId: {}", userId);

        User user = userRepository.findByKeycloakUserId(String.valueOf(userId))
                .orElseThrow(() -> new UserException("User not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (user.getProfilePhoto() == null || user.getProfilePhoto().length == 0) {
            throw new UserException("Profile photo not found", "PHOTO_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
        return user.getProfilePhoto();
    }

    @Override
    public UserResponseDTO deleteProfilePhoto(UUID userId) {
        log.info("Deleting profile photo for userId: {}", userId);

        User user = userRepository.findByKeycloakUserId(String.valueOf(userId))
                .orElseThrow(() -> new UserException("User not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (user.getProfilePhoto() == null) {
            throw new UserException("No profile photo to delete", "PHOTO_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
        user.setProfilePhoto(null);
        user.setPhotoContentType(null);
        User updatedUser = userRepository.save(user);
        UserResponseDTO response = userMapper.toUserResponseDTO(updatedUser);
        cacheUser(response);

        log.info("Successfully deleted profile photo for userId: {}", userId);
        return response;
    }

    @Override
    public void blockUser(UUID blockerId, UUID blockedId, String reason) {
        log.info("User {} blocking user {}", blockerId, blockedId);
        userRepository.findByKeycloakUserId(String.valueOf(blockerId))
                .orElseThrow(() -> new UserException("Blocker user not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        userRepository.findByKeycloakUserId(String.valueOf(blockedId))
                .orElseThrow(() -> new UserException("Blocked user not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (blockerId.equals(blockedId)) {
            throw new UserException(
                    "You cannot block yourself",
                    "INVALID_BLOCK_REQUEST",
                    HttpStatus.BAD_REQUEST
            );
        }
        if (userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            log.info("User {} already blocked user {}", blockerId, blockedId);
            return;
        }
        UserBlock block = UserBlock.builder()
                .blockerId(blockerId)
                .blockedId(blockedId)
                .build();

        userBlockRepository.save(block);
        log.info("User {} successfully blocked user {}", blockerId, blockedId);
    }

    @Override
    public void unblockUser(UUID blockerId, UUID blockedId) {
        log.info("User {} unblocking user {}", blockerId, blockedId);
        userBlockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
        log.info("User {} successfully unblocked user {}", blockerId, blockedId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getBlockedUsers(UUID userId) {
        log.debug("Getting blocked users for userId: {}", userId);
        List<String> blockedIds = userBlockRepository.findByBlockerId(userId)
                .stream()
                .map(UserBlock::getBlockedId)
                .map(UUID::toString)
                .toList();
        if (blockedIds.isEmpty()) {
            return List.of();
        }
        return userRepository.findAllByKeycloakUserIdIn(blockedIds)
                .stream()
                .map(userMapper::toUserResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserBlocked(UUID userId, UUID potentiallyBlockedId) {
        return userBlockRepository.existsByBlockerIdAndBlockedId(userId, potentiallyBlockedId) ||
                userBlockRepository.existsByBlockerIdAndBlockedId(potentiallyBlockedId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> getBlockedUserIds(UUID userId) {
        List<UUID> blockedByUser = userBlockRepository.findByBlockerId(userId)
                .stream()
                .map(UserBlock::getBlockedId)
                .toList();
        List<UUID> blockedUser = userBlockRepository.findByBlockedId(userId)
                .stream()
                .map(UserBlock::getBlockerId)
                .toList();
        return Stream.concat(blockedByUser.stream(), blockedUser.stream())
                .distinct()
                .toList();
    }

    @Override
    public ContentReportDTO reportUser(UUID reporterId, UUID reportedUserId, String details) {
        log.info("User {} reporting user {}", reporterId, reportedUserId);
        userRepository.findByKeycloakUserId(String.valueOf(reporterId))
                .orElseThrow(() -> new UserException("Reporter not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        userRepository.findByKeycloakUserId(String.valueOf(reportedUserId))
                .orElseThrow(() -> new UserException("Reported user not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));
        ContentReport report = ContentReport.builder()
                .reporterId(reporterId)
                .reportedUserId(reportedUserId)
                .details(details)
                .status(ReportStatus.PENDING)
                .build();

        ContentReport savedReport = contentReportRepository.save(report);
        log.info("User report created with ID: {}", savedReport.getId());
        return contentReportMapper.toContentReportDTO(savedReport);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContentReportDTO> getUserReports(UUID userId, Pageable pageable) {
        log.debug("Getting reports submitted by user: {}", userId);
        Page<ContentReport> reports = contentReportRepository.findByReporterId(userId, pageable);
        return reports.map(contentReportMapper::toContentReportDTO);
    }

    private void cacheUser(UserResponseDTO user) {
        try {
            String cacheKey = USER_CACHE_KEY_PREFIX + user.getId();
            redisTemplate.opsForValue().set(cacheKey, user, Duration.ofHours(24));
            log.debug("Successfully cached user: {}", user.getId());
        } catch (Exception e) {
            log.warn("Failed to cache user: {}", user.getId(), e);
        }
    }
}