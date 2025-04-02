package org.fivy.matchservice.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fivy.matchservice.domain.enums.AttendanceStatus;
import org.fivy.matchservice.domain.enums.ConfirmationMethod;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchAttendanceResponse {
    private UUID id;
    private UUID matchId;
    private UUID playerId;
    private String playerName;
    private ZonedDateTime confirmationTime;
    private ConfirmationMethod confirmationMethod;
    private UUID confirmedBy;
    private String confirmedByName;
    private AttendanceStatus status;
}