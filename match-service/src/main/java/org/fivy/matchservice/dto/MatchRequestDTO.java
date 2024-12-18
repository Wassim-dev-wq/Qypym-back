package org.fivy.matchservice.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.fivy.matchservice.enums.MatchVisibility;
import org.fivy.matchservice.enums.PlayerLevel;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MatchRequestDTO {

    @NotBlank(message = "Title is required")
    @Size(max = 100)
    private String title;

    @Size(max = 2000)
    private String description;

    @NotNull(message = "Date and time are required")
    private Instant dateTime;

    @NotNull(message = "Location ID is required")
    private UUID locationId;

    @Min(value = 4, message = "Match must have at least 4 players")
    @Max(value = 11, message = "Match can have at most 11 players")
    private Integer maxPlayers;

    @NotNull(message = "Visibility is required")
    private MatchVisibility matchVisibility;

    @NotNull(message = "Minimum level is required")
    private PlayerLevel playerLevel;

    @NotEmpty(message = "Required positions are required")
    private List<MatchRequiredPositionIdDTO> requiredPositions;
}
