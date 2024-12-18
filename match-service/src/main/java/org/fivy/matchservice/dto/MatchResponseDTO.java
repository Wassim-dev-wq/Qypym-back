package org.fivy.matchservice.dto;

import lombok.*;
import org.fivy.matchservice.enums.MatchStatus;
import org.fivy.matchservice.enums.MatchVisibility;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchResponseDTO {

    private UUID id;
    private String title;
    private String description;
    private Instant dateTime;
    private UUID locationId;
    private UUID creatorId;
    private int maxPlayers;
    private MatchStatus status;
    private MatchVisibility visibility;
    private String minimumLevel;
    private Instant createdAt;
    private Instant updatedAt;
    private List<MatchRequiredPositionIdDTO> requiredPositions;
}
