package org.fivy.matchservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "player_ratings",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"feedback_id", "rated_player_id", "rating_player_id"},
                name = "uk_player_ratings"
        ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerRating {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "feedback_id", nullable = false)
    private PlayerMatchFeedback feedback;

    @Column(name = "rated_player_id", nullable = false)
    private UUID ratedPlayerId;

    @Column(name = "rating_player_id", nullable = false)
    private UUID ratingPlayerId;

    @Column(name = "skill_rating")
    private Integer skillRating;

    @Column(name = "sportsmanship_rating")
    private Integer sportsmanshipRating;

    @Column(name = "teamwork_rating")
    private Integer teamworkRating;

    @Column(name = "reliability_rating")
    private Integer reliabilityRating;

    @Column(name = "comments")
    private String comments;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = ZonedDateTime.now();
    }
}