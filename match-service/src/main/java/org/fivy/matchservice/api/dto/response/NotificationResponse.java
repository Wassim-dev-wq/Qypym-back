package org.fivy.matchservice.api.dto.response;

import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponse {
    private UUID id;
    private UUID userId;
    private String title;
    private String message;
    private boolean read;
    private ZonedDateTime createdAt;
}
