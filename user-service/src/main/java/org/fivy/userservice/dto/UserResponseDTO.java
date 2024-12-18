package org.fivy.userservice.dto;

import lombok.*;
import org.fivy.userservice.enums.GenderIdentity;
import org.fivy.userservice.enums.PlayerLevel;
import org.fivy.userservice.enums.UserStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private UserStatus status;
    private PlayerLevel playerLevel;
    private LocalDate dateOfBirth;
    private GenderIdentity genderIdentity;
    private String bio;
    private String videoIntroUrl;
    private Double latitude;
    private Double longitude;
    private Integer preferredDistance;
    private Boolean isEmailVerified;
    private Boolean isPhoneVerified;
    private Boolean isProfileVerified;
    private Instant createdAt;
    private Instant updatedAt;
}
