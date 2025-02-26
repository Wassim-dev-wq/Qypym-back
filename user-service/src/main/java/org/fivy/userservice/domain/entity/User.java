package org.fivy.userservice.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.fivy.userservice.domain.enums.GenderIdentity;
import org.fivy.userservice.domain.enums.PlayerLevel;
import org.fivy.userservice.domain.enums.UserStatus;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_username", columnList = "username"),
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_phone_number", columnList = "phoneNumber"),
        @Index(name = "idx_user_keycloakUserId", columnList = "keycloakUserId")
})
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, unique = true)
    private String keycloakUserId;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(nullable = false, unique = true)
    private String email;


    @Size(max = 50)
    private String firstName;

    @Size(max = 50)
    private String lastName;

    @Size(max = 15)
    @Pattern(regexp = "\\+?[0-9]{7,15}")
    @Column(unique = true)
    private String phoneNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PlayerLevel playerLevel;

    @NotNull
    @Past
    private LocalDate dateOfBirth;

    @NotNull
    @Enumerated(EnumType.STRING)
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

    private Boolean isEmailVerified = false;

    private Boolean isPhoneVerified = false;

    private Boolean isProfileVerified = false;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
        status = UserStatus.ACTIVE;
        preferredDistance = 10;
        playerLevel = PlayerLevel.BEGINNER;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        User user = (User) o;
        return getId() != null && Objects.equals(getId(), user.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
