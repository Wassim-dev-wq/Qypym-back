package org.fivy.matchservice.domain.event.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchReminderEvent  {
    private UUID eventId;
    private UUID matchId;
    private UUID playerId;
    private UUID teamId;
    private String teamName;
    private String playerRole;
    private String matchTitle;
    private String matchLocation;
    private ZonedDateTime matchStartDate;
    private String matchFormat;
    private String skillLevel;
    private String eventType;
    private ZonedDateTime timestamp;
}