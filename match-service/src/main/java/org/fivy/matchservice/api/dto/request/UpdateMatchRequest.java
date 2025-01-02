package org.fivy.matchservice.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.fivy.matchservice.domain.enums.MatchType;

import java.time.LocalDateTime;

@Data
public class UpdateMatchRequest {
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private MatchType type;

    @Min(value = 2, message = "Minimum players must be at least 2")
    private Integer minPlayers;

    @Min(value = 2, message = "Maximum players must be at least 2")
    private Integer maxPlayers;

    @Min(value = 0, message = "Minimum player level must be non-negative")
    private Integer minPlayerLevel;

    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    private Double latitude;

    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private Double longitude;

    private String locationName;

    @Future(message = "Scheduled start time must be in the future")
    private LocalDateTime scheduledStartTime;

    @Future(message = "Scheduled end time must be in the future")
    private LocalDateTime scheduledEndTime;
}
