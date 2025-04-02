package org.fivy.matchservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "player_rating_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerRatingSummary {

    @Id
    @Column(name = "player_id")
    private UUID playerId;

    @Column(name = "total_matches", nullable = false)
    private int totalMatches;

    @Column(name = "avg_skill_rating", precision = 3, scale = 2, nullable = false)
    private BigDecimal avgSkillRating;

    @Column(name = "avg_sportsmanship_rating", precision = 3, scale = 2, nullable = false)
    private BigDecimal avgSportsmanshipRating;

    @Column(name = "avg_teamwork_rating", precision = 3, scale = 2, nullable = false)
    private BigDecimal avgTeamworkRating;

    @Column(name = "avg_reliability_rating", precision = 3, scale = 2, nullable = false)
    private BigDecimal avgReliabilityRating;

    @Column(name = "overall_rating", precision = 3, scale = 2, nullable = false)
    private BigDecimal overallRating;

    @Column(name = "total_ratings", nullable = false)
    private int totalRatings;

    @Column(name = "last_updated_at", nullable = false)
    private ZonedDateTime lastUpdatedAt;
}