package org.fivy.userservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fivy.userservice.enums.GenderIdentity;

import java.util.List;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferences {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_preferred_genders", joinColumns = @JoinColumn(name = "user_preferences_id"))
    @Column(name = "preferred_gender")
    private List<GenderIdentity> preferredGenders;

    @Min(18)
    @Max(100)
    private Integer ageRangeMin;

    @Min(18)
    @Max(100)
    private Integer ageRangeMax;

    @Min(1)
    private Integer distanceRange;

    @ElementCollection
    @CollectionTable(name = "user_interests", joinColumns = @JoinColumn(name = "user_preferences_id"))
    @Column(name = "interest")
    private List<String> interests;

    @ElementCollection
    @CollectionTable(name = "user_lifestyle_preferences", joinColumns = @JoinColumn(name = "user_preferences_id"))
    @Column(name = "lifestyle_preference")
    private List<String> lifestylePreferences;

    @Size(max = 100)
    private String educationLevel;

    @Size(max = 100)
    private String occupation;

    @ElementCollection
    @CollectionTable(name = "user_language_preferences", joinColumns = @JoinColumn(name = "user_preferences_id"))
    @Column(name = "language_preference")
    private List<String> languagePreferences;


}
