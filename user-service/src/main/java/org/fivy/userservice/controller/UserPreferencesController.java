package org.fivy.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fivy.userservice.dto.UserPreferencesDTO;
import org.fivy.userservice.service.UserPreferencesService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/{userId}/preferences")
@RequiredArgsConstructor
public class UserPreferencesController {

    private final UserPreferencesService preferencesService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserPreferencesDTO createUserPreferences(
            @PathVariable UUID userId,
            @Valid @RequestBody UserPreferencesDTO userPreferencesDTO){
        return preferencesService.createUserPreferences(userId, userPreferencesDTO);
    }

    @GetMapping
    public UserPreferencesDTO getUserPreferences(@PathVariable UUID userId) {
        return preferencesService.getUserPreferences(userId);
    }

    @PutMapping
    public UserPreferencesDTO updateUserPreferences(@PathVariable UUID userId,
                                                    @Valid @RequestBody UserPreferencesDTO preferencesDTO) {
        return preferencesService.updateUserPreferences(userId, preferencesDTO);
    }
}