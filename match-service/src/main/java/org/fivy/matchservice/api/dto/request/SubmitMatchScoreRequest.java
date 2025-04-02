package org.fivy.matchservice.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitMatchScoreRequest {

    @NotNull(message = "Team 1 ID is required")
    private UUID team1Id;

    @NotNull(message = "Team 2 ID is required")
    private UUID team2Id;

    @NotNull(message = "Team 1 score is required")
    @Min(value = 0, message = "Score cannot be negative")
    private Integer team1Score;

    @NotNull(message = "Team 2 score is required")
    @Min(value = 0, message = "Score cannot be negative")
    private Integer team2Score;
}