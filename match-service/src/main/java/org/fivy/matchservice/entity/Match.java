package org.fivy.matchservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.fivy.matchservice.enums.MatchStatus;
import org.fivy.matchservice.enums.MatchVisibility;
import org.fivy.matchservice.enums.PlayerLevel;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "\"Match\"")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Size(max = 100)
    @NotNull
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Size(max = 2000)
    @Column(name = "description", length = 2000)
    private String description;

    @NotNull
    @Column(name = "date", nullable = false)
    private Instant dateTime;

    @NotNull
    @Column(name = "maxplayers", nullable = false)
    private Integer maxPlayers;

    @NotNull
    @Column(name = "createdat", nullable = false)
    private Instant createdAt;

    @NotNull
    @Column(name = "updatedat", nullable = false)
    private Instant updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "minimumlevel", nullable = false)
    private PlayerLevel playerLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MatchStatus matchStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    private MatchVisibility matchVisibility;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        matchStatus = MatchStatus.SCHEDULED;
        matchVisibility = MatchVisibility.PUBLIC;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

}