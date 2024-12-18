package org.fivy.userservice.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import org.fivy.userservice.enums.GenderIdentity;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesDTO {

    private List<GenderIdentity> preferredGenders;

    @Min(18)
    @Max(100)
    private Integer ageRangeMin;

    @Min(18)
    @Max(100)
    private Integer ageRangeMax;

    @Min(1)
    private Integer distanceRange;

    private List<String> interests;

    private List<String> lifestylePreferences;

    @Size(max = 100)
    private String educationLevel;

    @Size(max = 100)
    private String occupation;

    private List<String> languagePreferences;
}