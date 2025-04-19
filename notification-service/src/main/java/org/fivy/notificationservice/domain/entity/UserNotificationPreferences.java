package org.fivy.notificationservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_notification_preferences",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id"})},
        indexes = {@Index(columnList = "user_id")})
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNotificationPreferences {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    @Builder.Default
    private boolean emailMatchReminders = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean emailMatchUpdates = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean emailPasswordReset = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean emailVerification = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean pushMatchJoinRequests = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean pushMatchInvitations = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean pushMatchUpdates = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean pushChatMessages = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean pushTeamUpdates = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean pushMatchReminders = true;

    @CreationTimestamp
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    private ZonedDateTime updatedAt;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        UserNotificationPreferences that = (UserNotificationPreferences) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}