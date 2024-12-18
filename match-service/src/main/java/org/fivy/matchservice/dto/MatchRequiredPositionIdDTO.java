package org.fivy.matchservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.UUID;

/**
 * DTO for {@link org.fivy.matchservice.entity.MatchRequiredPositionId}
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchRequiredPositionIdDTO {

    @NotNull(message = "Position ID is required")
    private UUID positionId;

    @Positive(message = "Quantity must be greater than 0")
    private int quantity;
}