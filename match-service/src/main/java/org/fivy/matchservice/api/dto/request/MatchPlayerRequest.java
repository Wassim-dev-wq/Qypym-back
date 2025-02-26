package org.fivy.matchservice.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fivy.matchservice.domain.enums.PlayerStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchPlayerRequest {
    private String matchId;
    private String playerId;
    private PlayerStatus status;
}
