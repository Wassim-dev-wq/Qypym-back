package org.fivy.matchservice.api.dto.response;

import lombok.Data;
import org.fivy.matchservice.domain.enums.PlayerRole;
import org.fivy.matchservice.domain.enums.PlayerStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MatchPlayerResponse {
    private UUID id;
    private UUID matchId;
    private UUID playerId;
    private UUID teamId;
    private String teamName;
    private Integer teamNumber;
    private String playerName;
    private String playerAvatar;
    private Integer playerLevel;
    private PlayerRole role;
    private PlayerStatus status;
    private LocalDateTime joinedAt;
}
