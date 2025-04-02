package org.fivy.matchservice.domain.event;

import org.fivy.matchservice.domain.enums.CommandType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class EmailVerificationEvent {
    private UUID matchId;
    private CommandType type;
    private Map<String, String> payload;
    private UUID requestId;
    private Instant timestamp;
}
