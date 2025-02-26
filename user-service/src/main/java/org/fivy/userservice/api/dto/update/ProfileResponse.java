package org.fivy.userservice.api.dto.update;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fivy.userservice.domain.enums.PlayerLevel;
import org.fivy.userservice.domain.enums.UserStatus;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private String phoneNumber;
    private String bio;
    private String dateOfBirth;
    private String genderIdentity;
    private String videoIntroUrl;
    private Double latitude;
    private Double longitude;
    private Integer preferredDistance;
    private PlayerLevel playerLevel;
    private UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}