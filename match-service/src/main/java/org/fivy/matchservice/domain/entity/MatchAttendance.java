package org.fivy.matchservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.fivy.matchservice.domain.enums.AttendanceStatus;
import org.fivy.matchservice.domain.enums.ConfirmationMethod;
import org.hibernate.proxy.HibernateProxy;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "match_attendance", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"match_id", "player_id"})
})
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MatchAttendance {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    @ToString.Exclude
    private Match match;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "confirmation_time")
    private ZonedDateTime confirmationTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "confirmation_method", nullable = false)
    private ConfirmationMethod confirmationMethod;

    @Column(name = "confirmed_by")
    private UUID confirmedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AttendanceStatus status;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        MatchAttendance that = (MatchAttendance) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}