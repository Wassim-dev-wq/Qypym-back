package org.fivy.matchservice.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.matchservice.api.dto.response.MatchAttendanceResponse;
import org.fivy.matchservice.api.dto.response.VerificationCodeResponse;
import org.fivy.matchservice.api.mapper.MatchAttendanceMapper;
import org.fivy.matchservice.application.service.AttendanceService;
import org.fivy.matchservice.application.service.schedulers.MatchCodeSchedulerService;
import org.fivy.matchservice.domain.entity.Match;
import org.fivy.matchservice.domain.entity.MatchAttendance;
import org.fivy.matchservice.domain.enums.AttendanceStatus;
import org.fivy.matchservice.domain.enums.ConfirmationMethod;
import org.fivy.matchservice.domain.repository.MatchAttendanceRepository;
import org.fivy.matchservice.domain.repository.MatchRepository;
import org.fivy.matchservice.shared.exception.MatchException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AttendanceServiceImpl implements AttendanceService {

    private final MatchRepository matchRepository;
    private final MatchAttendanceRepository attendanceRepository;
    private final MatchAttendanceMapper attendanceMapper;
    private final MatchCodeSchedulerService matchCodeSchedulerService;

    private static final long CODE_GENERATE_THRESHOLD_MINUTES = 60;

    @Override
    public VerificationCodeResponse generateVerificationCode(UUID matchId) {
        log.debug("Retrieving verification code for match: {}", matchId);
        Match match = findMatchOrThrow(matchId);
        ZonedDateTime now = ZonedDateTime.now();
        if (match.getVerificationCode() != null && match.getCodeExpiryTime() != null
                && match.getCodeExpiryTime().isAfter(now)) {
            return buildVerificationResponse(match);
        }
        if (match.getStartDate().minusMinutes(CODE_GENERATE_THRESHOLD_MINUTES).isAfter(now)) {
            log.info("Too early to generate verification code for match: {}", matchId);
            throw new MatchException(
                    "Verification codes are only available starting 1 hour before match time",
                    "CODE_NOT_AVAILABLE_YET",
                    HttpStatus.BAD_REQUEST);
        }

        matchCodeSchedulerService.generateCodeForMatch(match, now);
        match = matchRepository.findById(matchId).orElseThrow();
        log.info("Generated new verification code for match: {}", matchId);
        return buildVerificationResponse(match);
    }

    @Override
    public MatchAttendanceResponse confirmAttendanceViaCode(UUID matchId, String verificationCode, UUID playerId) {
        log.debug("Confirming attendance via code for match: {}, player: {}", matchId, playerId);
        Match match = findMatchOrThrow(matchId);
        ZonedDateTime now = ZonedDateTime.now();
        if (match.getVerificationCode() == null ||
                !match.getVerificationCode().equals(verificationCode) ||
                match.getCodeExpiryTime() == null ||
                match.getCodeExpiryTime().isBefore(now)) {

            log.warn("Invalid or expired verification code for match: {}", matchId);
            throw new MatchException("Invalid or expired verification code", "INVALID_CODE", HttpStatus.BAD_REQUEST);
        }
        MatchAttendance attendance = findOrCreateAttendance(match, playerId);
        attendance.setConfirmationTime(now);
        attendance.setConfirmationMethod(ConfirmationMethod.CODE);
        attendance.setStatus(AttendanceStatus.CONFIRMED);
        attendance.setUpdatedAt(now);
        MatchAttendance savedAttendance = attendanceRepository.save(attendance);
        log.info("Player {} confirmed attendance via code for match {}", playerId, matchId);
        return attendanceMapper.toAttendanceResponse(savedAttendance);
    }


    @Override
    public MatchAttendanceResponse confirmAttendanceManually(UUID matchId, UUID playerId, UUID confirmedBy) {
        log.debug("Manually confirming attendance for match: {}, player: {}, by: {}", matchId, playerId, confirmedBy);
        Match match = findMatchOrThrow(matchId);
        if (!match.getCreatorId().equals(confirmedBy)) {
            log.warn("User {} is not authorized to manually confirm attendance for match {}", confirmedBy, matchId);
            throw new MatchException("Only match creator can manually confirm attendance", "UNAUTHORIZED", HttpStatus.FORBIDDEN);
        }

        MatchAttendance attendance = findOrCreateAttendance(match, playerId);
        attendance.setConfirmationTime(ZonedDateTime.now());
        attendance.setConfirmationMethod(ConfirmationMethod.MANUAL);
        attendance.setConfirmedBy(confirmedBy);
        attendance.setStatus(AttendanceStatus.CONFIRMED);
        attendance.setUpdatedAt(ZonedDateTime.now());

        MatchAttendance savedAttendance = attendanceRepository.save(attendance);
        log.info("Player {} attendance manually confirmed for match {} by {}", playerId, matchId, confirmedBy);

        return attendanceMapper.toAttendanceResponse(savedAttendance);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchAttendanceResponse> getMatchAttendances(UUID matchId) {
        log.debug("Fetching attendances for match: {}", matchId);
        if (!matchRepository.existsById(matchId)) {
            throw new MatchException("Match not found", "MATCH_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
        List<MatchAttendance> attendances = attendanceRepository.findByMatchId(matchId);
        return attendanceMapper.toAttendanceResponseList(attendances);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceStatus getPlayerAttendanceStatus(UUID matchId, UUID playerId) {
        log.debug("Checking attendance status for match: {}, player: {}", matchId, playerId);

        Optional<MatchAttendance> attendance = attendanceRepository.findByMatchIdAndPlayerId(matchId, playerId);
        return attendance.map(MatchAttendance::getStatus).orElse(AttendanceStatus.NOT_CONFIRMED);
    }

    @Override
    @Transactional(readOnly = true)
    public long getAttendanceCount(UUID matchId) {
        log.debug("Counting confirmed attendances for match: {}", matchId);
        return attendanceRepository.countByMatchIdAndStatus(matchId, AttendanceStatus.CONFIRMED);
    }


    private Match findMatchOrThrow(UUID matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchException("Match not found", "MATCH_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private MatchAttendance findOrCreateAttendance(Match match, UUID playerId) {
        return attendanceRepository.findByMatchIdAndPlayerId(match.getId(), playerId)
                .orElseGet(() -> {
                    MatchAttendance newAttendance = MatchAttendance.builder()
                            .match(match)
                            .playerId(playerId)
                            .status(AttendanceStatus.NOT_CONFIRMED)
                            .createdAt(ZonedDateTime.now())
                            .build();
                    return attendanceRepository.save(newAttendance);
                });
    }

    private VerificationCodeResponse buildVerificationResponse(Match match) {
        long remainingMinutes = 0;
        if (match.getCodeExpiryTime() != null) {
            remainingMinutes = Duration.between(ZonedDateTime.now(), match.getCodeExpiryTime()).toMinutes();
            if (remainingMinutes < 0) {
                remainingMinutes = 0;
            }
        }

        return VerificationCodeResponse.builder()
                .matchId(match.getId())
                .verificationCode(match.getVerificationCode())
                .expiryTime(match.getCodeExpiryTime())
                .validForMinutes(remainingMinutes)
                .build();
    }
}