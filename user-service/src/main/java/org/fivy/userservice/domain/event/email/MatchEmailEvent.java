package org.fivy.userservice.domain.event.email;

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
public class MatchEmailEvent {
    private UUID userId;
    private String email;
    private String firstName;
    private String eventType;
    private UUID matchId;
    private String matchTitle;
    private ZonedDateTime matchStartDate;
    private String matchLocation;
    private String matchFormat;
    private String teamName;
    private String playerRole;
    private String verificationCode;
    private int verificationCodeTtl;
}