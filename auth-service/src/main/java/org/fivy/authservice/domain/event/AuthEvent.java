package org.fivy.authservice.domain.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fivy.authservice.domain.enums.AuthEventType;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthEvent implements Serializable {
    private String eventId = UUID.randomUUID().toString();
    private AuthEventType type;
    private String userId;
    private Map<String, Object> data;
    private LocalDateTime timestamp = LocalDateTime.now();
    private String correlationId;

    @JsonIgnore
    public String getPartitionKey() {
        return userId != null ? userId : eventId;
    }
}