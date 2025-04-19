package org.fivy.matchservice.api.dto.response;

import lombok.*;
import org.fivy.matchservice.domain.enums.MatchFormat;
import org.fivy.matchservice.domain.enums.MatchStatus;
import org.fivy.matchservice.domain.enums.SkillLevel;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchResponse {

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
    private boolean isJoined;
    private boolean saved;
    private int savedCount;
    private int joinedCount;
    private int maxPlayers;
    private int joinRequestCount;
    private double price;

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
}
