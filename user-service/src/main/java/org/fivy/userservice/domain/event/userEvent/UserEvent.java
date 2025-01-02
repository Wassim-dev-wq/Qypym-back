package org.fivy.userservice.domain.event.userEvent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent implements Serializable {
    private UUID eventId = UUID.randomUUID();
    private String keycloakUserId;
    private UUID userId;
    private UserEventType type;
    private Map<String, Object> data;
    private Instant timestamp = Instant.now();
}
