package org.fivy.matchservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "player_match_feedback",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"feedback_request_id", "player_id"},
                name = "uk_player_match_feedback"
        ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerMatchFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "feedback_request_id", nullable = false)
    private MatchFeedbackRequest feedbackRequest;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "submitted_at", nullable = false)
    private ZonedDateTime submittedAt;

    @Column(name = "match_rating")
    private Integer matchRating;

    @Column(name = "match_comments")
    private String matchComments;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @OneToMany(mappedBy = "feedback", cascade = CascadeType.ALL)
    private Set<PlayerRating> playerRatings = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = ZonedDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }
}