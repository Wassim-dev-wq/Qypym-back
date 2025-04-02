package org.fivy.matchservice.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerStatsResponse {
    private int totalMatches;
    private int createdMatches;
    private int upcomingMatches;
    private double reliabilityRate;
    private String preferredFormat;
    private String preferredRole;
    private Map<String, Integer> roleDistribution;
    private int teamCount;
    private double teamSwitchRate;
    private Map<String, Integer> activityByMonth;
    private int savedMatches;
    private Map<String, Integer> matchesCountByStatus;
    private Map<String, Integer> formatDistribution;
    private Map<String, Integer> skillDistribution;
}