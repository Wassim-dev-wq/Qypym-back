package org.fivy.matchservice.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fivy.matchservice.domain.enums.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchHistoryResponse {
    private UUID matchId;
    private String title;
    private MatchFormat format;
    private SkillLevel skillLevel;
    private ZonedDateTime startDate;
    private Integer duration;
    private MatchStatus status;
    private UUID creatorId;
    private Boolean isCreator;

    private String address;
    private Double latitude;
    private Double longitude;

    private PlayerRole playerRole;
    private PlayerStatus playerStatus;
    private LocalDateTime joinedAt;
    private UUID playerTeamId;
    private String playerTeamName;
    private Integer playerTeamNumber;

    private List<TeamInfo> teams;

    private ResultInfo result;

    private WeatherInfo weather;

    private FeedbackInfo feedback;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TeamInfo {
        private UUID teamId;
        private String teamName;
        private Integer teamNumber;
        private List<PlayerInfo> players;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PlayerInfo {
        private UUID playerId;
        private PlayerRole role;
        private PlayerStatus status;
        private LocalDateTime joinedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResultInfo {
        private MatchResultStatus status;
        private Integer team1Score;
        private Integer team2Score;
        private UUID winningTeamId;
        private String winningTeamName;
        private Integer winningTeamNumber;
        private ZonedDateTime confirmedAt;

        private Boolean userSubmittedScore;
        private Integer userSubmittedTeam1Score;
        private Integer userSubmittedTeam2Score;
        private ScoreSubmissionStatus userScoreSubmissionStatus;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeatherInfo {
        private Integer temperature;
        private String condition;
        private Integer humidity;
        private Integer windSpeed;
        private Integer cloudCoverage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FeedbackInfo {
        private FeedbackRequestStatus requestStatus;
        private ZonedDateTime expiryAt;

        private Boolean userSubmittedFeedback;
        private Integer userMatchRating;
        private String userMatchComments;
        private ZonedDateTime userFeedbackSubmittedAt;

        private List<RatingInfo> ratingsReceived;
        private Double avgSkillRating;
        private Double avgSportsmanshipRating;
        private Double avgTeamworkRating;
        private Double avgReliabilityRating;
        private Double avgOverallRating;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RatingInfo {
        private UUID ratingPlayerId;
        private Integer skillRating;
        private Integer sportsmanshipRating;
        private Integer teamworkRating;
        private Integer reliabilityRating;
        private String comments;
    }
}