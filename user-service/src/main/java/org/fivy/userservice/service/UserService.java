package org.fivy.userservice.service;

import org.fivy.userservice.dto.UserRequestDTO;
import org.fivy.userservice.dto.UserResponseDTO;

import java.util.UUID;

public interface UserService {

    UserResponseDTO createUserProfile(UserRequestDTO userRequestDTO);

    UserResponseDTO getUserById(UUID userId);

    UserResponseDTO updateUser(UUID userId, UserRequestDTO userRequestDTO);

    void deleteUser(UUID userId);
}
