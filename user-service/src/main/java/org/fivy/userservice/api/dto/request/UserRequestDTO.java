package org.fivy.userservice.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import org.fivy.userservice.domain.enums.GenderIdentity;
import org.fivy.userservice.domain.enums.PlayerLevel;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDTO {

    @NotBlank
    @Size(max = 255)
    private String keycloakUserId;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 50)
    private String firstName;

    @Size(max = 50)
    private String lastName;

    @Size(max = 15)
    @Pattern(regexp = "\\+?[0-9]{7,15}")
    private String phoneNumber;

    @NotNull
    private PlayerLevel playerLevel;

    @NotNull
    @Past
    private LocalDate dateOfBirth;

    @NotNull
    private GenderIdentity genderIdentity;

    @Size(max = 500)
    private String bio;

    @Size(max = 255)
    private String videoIntroUrl;

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double latitude;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double longitude;

    @Min(1)
    private Integer preferredDistance;
}