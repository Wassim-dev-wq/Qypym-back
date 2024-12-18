package org.fivy.matchservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.fivy.matchservice.enums.PlayerLevel;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "\"MatchParticipant\"")
public class MatchParticipant {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "matchId", nullable = false)
    private UUID matchId;

    @Column(name = "playerLevel", nullable = false)
    private PlayerLevel playerLevel;

    @Column(name = "joinedAt", nullable = false)
    private Instant joinedAt;

    @PrePersist
    public void prePersist() {
        joinedAt = Instant.now();
    }
}