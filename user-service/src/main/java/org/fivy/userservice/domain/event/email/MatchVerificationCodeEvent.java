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
public class MatchVerificationCodeEvent{
    private UUID eventId;
    private UUID matchId;
    private UUID creatorId;
    private String verificationCode;
    private ZonedDateTime expiryTime;
    private int verificationCodeTtl;
    private String matchTitle;
    private String matchLocation;
    private ZonedDateTime matchStartDate;
    private String matchFormat;
}