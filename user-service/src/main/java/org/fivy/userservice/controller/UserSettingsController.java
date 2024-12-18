package org.fivy.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fivy.userservice.dto.UserSettingsDto;
import org.fivy.userservice.service.UserSettingsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/{userId}/settings")
@RequiredArgsConstructor
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserSettingsDto createUserSettings(@PathVariable UUID userId,
                                              @Valid @RequestBody UserSettingsDto userSettingsDto) {
        return userSettingsService.createUserSettings(userId, userSettingsDto);
    }

    @GetMapping
    public UserSettingsDto getUserSettings(@PathVariable UUID userId) {
        return userSettingsService.getUserSettings(userId);
    }

}
