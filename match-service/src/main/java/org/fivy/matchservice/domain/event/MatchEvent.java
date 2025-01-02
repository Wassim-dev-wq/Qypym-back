package org.fivy.matchservice.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchEvent {
    private UUID id;
    private MatchEventType type;
    private UUID matchId;
    private UUID creatorId;
    private Map<String, String> data;
    private LocalDateTime timestamp;
}

