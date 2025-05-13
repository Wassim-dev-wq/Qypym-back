package org.fivy.userservice.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.fivy.userservice.domain.enums.GenderIdentity;
import org.fivy.userservice.domain.enums.PlayerLevel;
import org.fivy.userservice.domain.enums.UserStatus;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO implements Serializable {

    private UUID id;
    private String keycloakUserId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private UserStatus status;
    private PlayerLevel playerLevel;
    @JsonFormat(pattern = "yyyy-MM-dd")
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
    private byte[] profilePhoto;
    private String photoContentType;
    private String photoFilename;
    private Instant createdAt;
    private Instant updatedAt;
}
