package org.fivy.notificationservice.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
public class UserPushTokenResponse {
    private UUID id;
    private UUID userId;
    private String expoToken;
    private ZonedDateTime createdAt;
}
