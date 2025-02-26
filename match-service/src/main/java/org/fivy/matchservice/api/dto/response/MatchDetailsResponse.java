package org.fivy.matchservice.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fivy.matchservice.domain.enums.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchDetailsResponse implements Serializable {
    private UUID id;
    private String title;
    private ZonedDateTime startDate;
    private Integer duration;
    private MatchFormat format;
    private LocationDTO location;
    private SkillLevel skillLevel;
    private MatchStatus status;
    private UUID creatorId;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private boolean isOwner;
    private boolean saved;
    private long savedCount;
    private List<MatchPlayerResponse> players;
    private List<MatchTeamResponse> teams;
    private int maxPlayers;
    private int maxPlayersPerTeam;
    private int currentPlayers;
    private WeatherResponse weather;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocationDTO {
        private String address;
        private CoordinatesDTO coordinates;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CoordinatesDTO {
        private Double latitude;
        private Double longitude;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MatchTeamResponse {
        private UUID id;
        private String name;
        private int teamNumber;
        private List<MatchPlayerResponse> players;
        private int currentPlayers;
        private int maxPlayers;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MatchPlayerResponse {
        private UUID id;
        private UUID playerId;
        private String nickname;
        private String avatarUrl;
        private PlayerRole role;
        private PlayerStatus status;
        private TeamInfo team;
        private LocalDateTime joinedAt;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class TeamInfo {
            private UUID id;
            private String name;
            private int teamNumber;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeatherResponse {
        private Integer temperature;
        private String condition;
        private Integer humidity;
        private Integer windSpeed;
        private Integer cloudCoverage;
        private Integer weatherId;
    }
}