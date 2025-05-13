package org.fivy.userservice.application.service;

import org.fivy.userservice.api.dto.ContentReportDTO;
import org.fivy.userservice.api.dto.UserRequestDTO;
import org.fivy.userservice.api.dto.UserResponseDTO;
import org.fivy.userservice.api.dto.update.ProfileResponse;
import org.fivy.userservice.api.dto.update.UpdateProfileRequest;
import org.fivy.userservice.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponseDTO createUserProfile(UserRequestDTO userRequestDTO);

    UserResponseDTO createUserFromAuthEvent(User userFromEvent);

    UserResponseDTO getUserById(UUID userId);

    UserResponseDTO updateProfile(UUID userId, UpdateProfileRequest request);

    void deleteUser(UUID userId);
    UserResponseDTO uploadProfilePhoto(UUID userId, MultipartFile file) throws IOException;
    byte[] getProfilePhoto(UUID userId);

    UserResponseDTO deleteProfilePhoto(UUID userId);
    void blockUser(UUID blockerId, UUID blockedId, String reason);
    void unblockUser(UUID blockerId, UUID blockedId);
    List<UserResponseDTO> getBlockedUsers(UUID userId);
    boolean isUserBlocked(UUID userId, UUID potentiallyBlockedId);
    List<UUID> getBlockedUserIds(UUID userId);
    ContentReportDTO reportUser(UUID reporterId, UUID reportedUserId, String details);
    Page<ContentReportDTO> getUserReports(UUID userId, Pageable pageable);
}
