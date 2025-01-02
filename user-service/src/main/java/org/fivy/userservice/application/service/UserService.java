package org.fivy.userservice.application.service;

import org.fivy.userservice.api.dto.UserRequestDTO;
import org.fivy.userservice.api.dto.UserResponseDTO;
import org.fivy.userservice.api.dto.update.ProfileResponse;
import org.fivy.userservice.api.dto.update.UpdateProfileRequest;
import org.fivy.userservice.domain.entity.User;

import java.util.UUID;

public interface UserService {

    UserResponseDTO createUserProfile(UserRequestDTO userRequestDTO);

    UserResponseDTO createUserFromAuthEvent(User userFromEvent);

    UserResponseDTO getUserById(UUID userId);

    ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);

    void deleteUser(UUID userId);
}
