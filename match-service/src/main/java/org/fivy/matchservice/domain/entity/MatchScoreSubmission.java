package org.fivy.matchservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.fivy.matchservice.domain.enums.ScoreSubmissionStatus;
import org.hibernate.annotations.GenericGenerator;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "match_score_submissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchScoreSubmission {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(name = "submitter_id", nullable = false)
    private UUID submitterId;

    @ManyToOne
    @JoinColumn(name = "team1_id", nullable = false)
    private MatchTeam team1;

    @ManyToOne
    @JoinColumn(name = "team2_id", nullable = false)
    private MatchTeam team2;

    @Column(name = "team1_score", nullable = false)
    private Integer team1Score;

    @Column(name = "team2_score", nullable = false)
    private Integer team2Score;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ScoreSubmissionStatus status;
}