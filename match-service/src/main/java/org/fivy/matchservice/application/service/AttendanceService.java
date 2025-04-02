package org.fivy.matchservice.application.service;

import org.fivy.matchservice.api.dto.response.MatchAttendanceResponse;
import org.fivy.matchservice.api.dto.response.VerificationCodeResponse;
import org.fivy.matchservice.domain.enums.AttendanceStatus;

import java.util.List;
import java.util.UUID;

public interface AttendanceService {

    VerificationCodeResponse generateVerificationCode(UUID matchId);

    MatchAttendanceResponse confirmAttendanceViaCode(UUID matchId, String verificationCode, UUID playerId);

    MatchAttendanceResponse confirmAttendanceManually(UUID matchId, UUID playerId, UUID confirmedBy);

    List<MatchAttendanceResponse> getMatchAttendances(UUID matchId);

    AttendanceStatus getPlayerAttendanceStatus(UUID matchId, UUID playerId);

    long getAttendanceCount(UUID matchId);
}