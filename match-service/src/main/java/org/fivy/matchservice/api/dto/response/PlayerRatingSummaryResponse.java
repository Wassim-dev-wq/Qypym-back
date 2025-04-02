package org.fivy.matchservice.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerRatingSummaryResponse {
    private UUID playerId;
    private BigDecimal overallRating;
    private BigDecimal skillRating;
    private BigDecimal sportsmanshipRating;
    private BigDecimal teamworkRating;
    private BigDecimal reliabilityRating;
    private int totalMatches;
    private int totalRatings;
    private ZonedDateTime lastUpdatedAt;
}