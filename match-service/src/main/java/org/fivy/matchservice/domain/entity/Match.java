package org.fivy.matchservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.fivy.matchservice.domain.enums.MatchFormat;
import org.fivy.matchservice.domain.enums.MatchStatus;
import org.fivy.matchservice.domain.enums.SkillLevel;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "start_date", nullable = false)
    private ZonedDateTime startDate;

    @Column(nullable = false)
    private Integer duration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchFormat format;

    @Embedded
    private Location location;

    @Column(name = "max_players", nullable = false)
    private int maxPlayers;

    @Column(name = "max_players_per_team", nullable = false)
    private int maxPlayersPerTeam;

    @OneToMany(mappedBy = "match")
    private Set<MatchTeam> teams = new HashSet<>();

    @OneToMany(mappedBy = "match")
    private Set<MatchPlayer> players = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MatchStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level", nullable = false)
    private SkillLevel skillLevel;

    @OneToOne(mappedBy = "match", cascade = CascadeType.ALL)
    private MatchWeather weather;

    @Column(name = "creator_id", nullable = false)
    private UUID creatorId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false;

    private ZonedDateTime deletedAt;
    private UUID deletedBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = ZonedDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }

    public int getMaxPlayers() {
        return format.getMaxPlayers();
    }

    @Transient
    public int getPlayersPerTeam() {
        return format.getMaxPlayers() / 2;
    }
}