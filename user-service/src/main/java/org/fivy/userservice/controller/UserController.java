package org.fivy.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fivy.userservice.dto.UserRequestDTO;
import org.fivy.userservice.dto.UserResponseDTO;
import org.fivy.userservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/profile")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDTO createUserProfile(@Valid @RequestBody UserRequestDTO userRequestDTO,
                                             @RequestHeader("X-User-ID") String userId,
                                             @RequestHeader("X-User-Email") String email,
                                             @RequestHeader("X-User-Name") String name,
                                             @RequestHeader("X-User-Roles") String roles) {
        userRequestDTO.setKeycloakUserId(userId);
        userRequestDTO.setEmail(email);
        return userService.createUserProfile(userRequestDTO);
    }

    @GetMapping("/{userId}")
    public UserResponseDTO getUserById(@PathVariable UUID userId) {
        return userService.getUserById(userId);
    }

    @PutMapping("/{userId}")
    public UserResponseDTO updateUser(@PathVariable UUID userId, @Valid @RequestBody UserRequestDTO userRequestDTO) {
        return userService.updateUser(userId, userRequestDTO);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
    }
}
