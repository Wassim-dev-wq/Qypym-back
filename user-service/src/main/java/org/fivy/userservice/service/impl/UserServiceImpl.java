package org.fivy.userservice.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.fivy.userservice.dto.UserRequestDTO;
import org.fivy.userservice.dto.UserResponseDTO;
import org.fivy.userservice.entity.User;
import org.fivy.userservice.enums.UserStatus;
import org.fivy.userservice.mapper.UserMapper;
import org.fivy.userservice.repository.UserRepository;
import org.fivy.userservice.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;


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
        userMapper.toUser(userRequestDTO);
        User savedUser = userRepository.save(user);
        return userMapper.toUserResponseDTO(savedUser);
    }

    @Override
    public UserResponseDTO getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
        return userMapper.toUserResponseDTO(user);
    }

    @Transactional
    @Override
    public UserResponseDTO updateUser(UUID userId, UserRequestDTO userRequestDTO) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        userMapper.updateUserFromDto(userRequestDTO, existingUser);


        User updatedUser = userRepository.save(existingUser);
        return userMapper.toUserResponseDTO(updatedUser);
    }

    @Transactional
    @Override
    public void deleteUser(UUID userId) {
        userRepository.updateUserStatus(userId, UserStatus.DELETED);
    }
}
