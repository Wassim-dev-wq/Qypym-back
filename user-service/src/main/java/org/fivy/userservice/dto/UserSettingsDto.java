package org.fivy.userservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fivy.userservice.entity.UserSettings;
import org.fivy.userservice.enums.PrivacyVisibility;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for {@link UserSettings}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsDto {
    @NotNull
    Boolean notificationsEnabled;
    @NotNull
    Boolean emailNotificationsEnabled;
    @NotNull
    Boolean pushNotificationEnabled;
    @NotNull
    PrivacyVisibility privacyVisibility;
    Instant createdAt;
    Instant updatedAt;
}