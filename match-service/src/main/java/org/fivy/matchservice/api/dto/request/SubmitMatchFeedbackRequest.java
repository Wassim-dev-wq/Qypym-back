package org.fivy.matchservice.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitMatchFeedbackRequest {

    @Min(value = 1, message = "Match rating must be between 1 and 5")
    @Max(value = 5, message = "Match rating must be between 1 and 5")
    private Integer matchRating;

    @Size(max = 500, message = "Comments cannot exceed 500 characters")
    private String matchComments;

    @Valid
    private List<PlayerRatingRequest> playerRatings;

    private UUID team1Id;
    private UUID team2Id;

    @Min(value = 0, message = "Score cannot be negative")
    private Integer team1Score;

    @Min(value = 0, message = "Score cannot be negative")
    private Integer team2Score;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerRatingRequest {
        @NotNull(message = "Rated player ID is required")
        private UUID ratedPlayerId;

        @Min(value = 1, message = "Skill rating must be between 1 and 5")
        @Max(value = 5, message = "Skill rating must be between 1 and 5")
        private Integer skillRating;

        @Min(value = 1, message = "Sportsmanship rating must be between 1 and 5")
        @Max(value = 5, message = "Sportsmanship rating must be between 1 and 5")
        private Integer sportsmanshipRating;

        @Min(value = 1, message = "Teamwork rating must be between 1 and 5")
        @Max(value = 5, message = "Teamwork rating must be between 1 and 5")
        private Integer teamworkRating;

        @Min(value = 1, message = "Reliability rating must be between 1 and 5")
        @Max(value = 5, message = "Reliability rating must be between 1 and 5")
        private Integer reliabilityRating;

        @Size(max = 200, message = "Comments cannot exceed 200 characters")
        private String comments;
    }
}