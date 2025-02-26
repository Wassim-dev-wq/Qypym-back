package org.fivy.matchservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.fivy.matchservice.domain.enums.JoinRequestStatus;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "match_join_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchJoinRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_team_id")
    private MatchTeam preferredTeam;

    @Column(length = 500)
    private String message;

    @Column(length = 50)
    private String position;

    @Column(length = 20)

    private String experience;

    @Column(length = 200)
    private String personalNote;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status", nullable = false)
    private JoinRequestStatus requestStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = ZonedDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }
}
