package org.fivy.matchservice.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fivy.matchservice.domain.enums.MatchResultStatus;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResultResponse {
    private UUID id;
    private UUID matchId;
    private String matchTitle;
    private MatchResultStatus status;
    private UUID winningTeamId;
    private String winningTeamName;
    private TeamScoreDTO team1;
    private TeamScoreDTO team2;
    private ZonedDateTime confirmedAt;
    private boolean userHasSubmitted;
    private int totalSubmissions;
    private int requiredSubmissions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamScoreDTO {
        private UUID teamId;
        private String teamName;
        private Integer score;
    }
}