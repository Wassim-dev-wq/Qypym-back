package org.fivy.matchservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.fivy.matchservice.domain.enums.FeedbackRequestStatus;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "match_feedback_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchFeedbackRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "match_id", nullable = false, unique = true)
    private Match match;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "expiry_at", nullable = false)
    private ZonedDateTime expiryAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackRequestStatus status;

    @OneToMany(mappedBy = "feedbackRequest", cascade = CascadeType.ALL)
    private Set<PlayerMatchFeedback> playerFeedbacks = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = ZonedDateTime.now();
    }
}