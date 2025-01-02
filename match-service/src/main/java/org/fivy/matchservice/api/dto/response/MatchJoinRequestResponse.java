package org.fivy.matchservice.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fivy.matchservice.domain.enums.JoinRequestStatus;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MatchJoinRequestResponse {
    private UUID id;
    private UUID matchId;
    private UUID userId;
    private JoinRequestStatus requestStatus;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}