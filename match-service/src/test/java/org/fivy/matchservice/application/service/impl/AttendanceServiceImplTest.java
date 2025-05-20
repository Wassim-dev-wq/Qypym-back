package org.fivy.matchservice.application.service.impl;

import org.fivy.matchservice.api.dto.response.MatchAttendanceResponse;
import org.fivy.matchservice.api.dto.response.VerificationCodeResponse;
import org.fivy.matchservice.api.mapper.MatchAttendanceMapper;
import org.fivy.matchservice.application.service.schedulers.MatchCodeSchedulerService;
import org.fivy.matchservice.domain.entity.Match;
import org.fivy.matchservice.domain.entity.MatchAttendance;
import org.fivy.matchservice.domain.enums.AttendanceStatus;
import org.fivy.matchservice.domain.enums.ConfirmationMethod;
import org.fivy.matchservice.domain.repository.MatchAttendanceRepository;
import org.fivy.matchservice.domain.repository.MatchRepository;
import org.fivy.matchservice.shared.exception.MatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AttendanceServiceImplTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchAttendanceRepository attendanceRepository;

    @Mock
    private MatchAttendanceMapper attendanceMapper;

    @Mock
    private MatchCodeSchedulerService matchCodeSchedulerService;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    private UUID matchId;
    private UUID playerId;
    private UUID creatorId;
    private Match match;
    private MatchAttendance attendance;
    private MatchAttendanceResponse attendanceResponse;
    private VerificationCodeResponse verificationCodeResponse;
    private String verificationCode;

    @BeforeEach
    void setUp() {
        matchId = UUID.randomUUID();
        playerId = UUID.randomUUID();
        creatorId = UUID.randomUUID();
        verificationCode = "123456";


        match = new Match();
        match.setId(matchId);
        match.setCreatorId(creatorId);
        match.setVerificationCode(verificationCode);
        match.setCodeExpiryTime(ZonedDateTime.now().plusMinutes(15));
        match.setStartDate(ZonedDateTime.now().plusMinutes(30));


        attendance = MatchAttendance.builder()
                .id(UUID.randomUUID())
                .match(match)
                .playerId(playerId)
                .status(AttendanceStatus.NOT_CONFIRMED)
                .createdAt(ZonedDateTime.now())
                .build();


        attendanceResponse = new MatchAttendanceResponse();
        attendanceResponse.setMatchId(matchId);
        attendanceResponse.setPlayerId(playerId);
        attendanceResponse.setStatus(AttendanceStatus.CONFIRMED);

        verificationCodeResponse = VerificationCodeResponse.builder()
                .matchId(matchId)
                .verificationCode(verificationCode)
                .expiryTime(match.getCodeExpiryTime())
                .validForMinutes(15)
                .build();
    }

    @Test
    void generateVerificationCode_CodeExists_ReturnsExistingCode() {

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));


        VerificationCodeResponse result = attendanceService.generateVerificationCode(matchId);


        assertNotNull(result);
        assertEquals(matchId, result.getMatchId());
        assertEquals(verificationCode, result.getVerificationCode());


        verify(matchRepository).findById(matchId);
        verify(matchCodeSchedulerService, never()).generateCodeForMatch(any(), any());
    }

    @Test
    void generateVerificationCode_CodeDoesNotExist_GeneratesNewCode() {

        match.setVerificationCode(null);
        match.setCodeExpiryTime(null);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        doNothing().when(matchCodeSchedulerService).generateCodeForMatch(any(), any());


        Match updatedMatch = new Match();
        updatedMatch.setId(matchId);
        updatedMatch.setVerificationCode(verificationCode);
        updatedMatch.setCodeExpiryTime(ZonedDateTime.now().plusMinutes(15));
        updatedMatch.setStartDate(ZonedDateTime.now().plusMinutes(30));

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match)).thenReturn(Optional.of(updatedMatch));


        VerificationCodeResponse result = attendanceService.generateVerificationCode(matchId);


        assertNotNull(result);
        assertEquals(matchId, result.getMatchId());
        assertEquals(verificationCode, result.getVerificationCode());


        verify(matchRepository, times(2)).findById(matchId);
        verify(matchCodeSchedulerService).generateCodeForMatch(eq(match), any(ZonedDateTime.class));
    }

    @Test
    void generateVerificationCode_TooEarlyToGenerate_ThrowsException() {

        match.setVerificationCode(null);
        match.setCodeExpiryTime(null);
        match.setStartDate(ZonedDateTime.now().plusHours(2));

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));


        MatchException exception = assertThrows(MatchException.class, () ->
                attendanceService.generateVerificationCode(matchId));

        assertEquals("Verification codes are only available starting 1 hour before match time", exception.getMessage());
        assertEquals("CODE_NOT_AVAILABLE_YET", exception.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());


        verify(matchRepository).findById(matchId);
        verify(matchCodeSchedulerService, never()).generateCodeForMatch(any(), any());
    }

    @Test
    void confirmAttendanceViaCode_ValidCode_ConfirmsAttendance() {

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(attendanceRepository.findByMatchIdAndPlayerId(matchId, playerId)).thenReturn(Optional.of(attendance));
        when(attendanceRepository.save(any(MatchAttendance.class))).thenReturn(attendance);
        when(attendanceMapper.toAttendanceResponse(any(MatchAttendance.class))).thenReturn(attendanceResponse);


        MatchAttendanceResponse result = attendanceService.confirmAttendanceViaCode(matchId, verificationCode, playerId);


        assertNotNull(result);
        assertEquals(matchId, result.getMatchId());
        assertEquals(playerId, result.getPlayerId());
        assertEquals(AttendanceStatus.CONFIRMED, result.getStatus());


        verify(matchRepository).findById(matchId);
        verify(attendanceRepository).findByMatchIdAndPlayerId(matchId, playerId);

        ArgumentCaptor<MatchAttendance> attendanceCaptor = ArgumentCaptor.forClass(MatchAttendance.class);
        verify(attendanceRepository).save(attendanceCaptor.capture());
        MatchAttendance savedAttendance = attendanceCaptor.getValue();

        assertEquals(AttendanceStatus.CONFIRMED, savedAttendance.getStatus());
        assertEquals(ConfirmationMethod.CODE, savedAttendance.getConfirmationMethod());
        assertNotNull(savedAttendance.getConfirmationTime());

        verify(attendanceMapper).toAttendanceResponse(attendance);
    }

    @Test
    void confirmAttendanceViaCode_InvalidCode_ThrowsException() {

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));


        MatchException exception = assertThrows(MatchException.class, () ->
                attendanceService.confirmAttendanceViaCode(matchId, "wrong-code", playerId));

        assertEquals("Invalid or expired verification code", exception.getMessage());
        assertEquals("INVALID_CODE", exception.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());


        verify(matchRepository).findById(matchId);
        verify(attendanceRepository, never()).findByMatchIdAndPlayerId(any(), any());
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void confirmAttendanceViaCode_ExpiredCode_ThrowsException() {

        match.setCodeExpiryTime(ZonedDateTime.now().minusMinutes(1));

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));


        MatchException exception = assertThrows(MatchException.class, () ->
                attendanceService.confirmAttendanceViaCode(matchId, verificationCode, playerId));

        assertEquals("Invalid or expired verification code", exception.getMessage());
        assertEquals("INVALID_CODE", exception.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());


        verify(matchRepository).findById(matchId);
        verify(attendanceRepository, never()).findByMatchIdAndPlayerId(any(), any());
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void confirmAttendanceViaCode_NewAttendance_CreatesAndConfirmsAttendance() {

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(attendanceRepository.findByMatchIdAndPlayerId(matchId, playerId)).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(MatchAttendance.class))).thenReturn(attendance);
        when(attendanceMapper.toAttendanceResponse(any(MatchAttendance.class))).thenReturn(attendanceResponse);


        MatchAttendanceResponse result = attendanceService.confirmAttendanceViaCode(matchId, verificationCode, playerId);


        assertNotNull(result);
        assertEquals(matchId, result.getMatchId());
        assertEquals(playerId, result.getPlayerId());


        verify(matchRepository).findById(matchId);
        verify(attendanceRepository).findByMatchIdAndPlayerId(matchId, playerId);


        verify(attendanceRepository, times(2)).save(any(MatchAttendance.class));
        verify(attendanceMapper).toAttendanceResponse(any(MatchAttendance.class));
    }

    @Test
    void confirmAttendanceManually_AsCreator_ConfirmsAttendance() {

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(attendanceRepository.findByMatchIdAndPlayerId(matchId, playerId)).thenReturn(Optional.of(attendance));
        when(attendanceRepository.save(any(MatchAttendance.class))).thenReturn(attendance);
        when(attendanceMapper.toAttendanceResponse(any(MatchAttendance.class))).thenReturn(attendanceResponse);


        MatchAttendanceResponse result = attendanceService.confirmAttendanceManually(matchId, playerId, creatorId);


        assertNotNull(result);
        assertEquals(matchId, result.getMatchId());
        assertEquals(playerId, result.getPlayerId());


        verify(matchRepository).findById(matchId);
        verify(attendanceRepository).findByMatchIdAndPlayerId(matchId, playerId);

        ArgumentCaptor<MatchAttendance> attendanceCaptor = ArgumentCaptor.forClass(MatchAttendance.class);
        verify(attendanceRepository).save(attendanceCaptor.capture());
        MatchAttendance savedAttendance = attendanceCaptor.getValue();

        assertEquals(AttendanceStatus.CONFIRMED, savedAttendance.getStatus());
        assertEquals(ConfirmationMethod.MANUAL, savedAttendance.getConfirmationMethod());
        assertEquals(creatorId, savedAttendance.getConfirmedBy());
        assertNotNull(savedAttendance.getConfirmationTime());

        verify(attendanceMapper).toAttendanceResponse(attendance);
    }

    @Test
    void confirmAttendanceManually_NotCreator_ThrowsException() {

        UUID nonCreatorId = UUID.randomUUID();

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));


        MatchException exception = assertThrows(MatchException.class, () ->
                attendanceService.confirmAttendanceManually(matchId, playerId, nonCreatorId));

        assertEquals("Only match creator can manually confirm attendance", exception.getMessage());
        assertEquals("UNAUTHORIZED", exception.getErrorCode());
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());


        verify(matchRepository).findById(matchId);
        verify(attendanceRepository, never()).findByMatchIdAndPlayerId(any(), any());
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void getMatchAttendances_Success() {

        List<MatchAttendance> attendances = Arrays.asList(attendance);
        List<MatchAttendanceResponse> attendanceResponses = Arrays.asList(attendanceResponse);

        when(matchRepository.existsById(matchId)).thenReturn(true);
        when(attendanceRepository.findByMatchId(matchId)).thenReturn(attendances);
        when(attendanceMapper.toAttendanceResponseList(attendances)).thenReturn(attendanceResponses);


        List<MatchAttendanceResponse> result = attendanceService.getMatchAttendances(matchId);


        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(matchId, result.get(0).getMatchId());
        assertEquals(playerId, result.get(0).getPlayerId());


        verify(matchRepository).existsById(matchId);
        verify(attendanceRepository).findByMatchId(matchId);
        verify(attendanceMapper).toAttendanceResponseList(attendances);
    }

    @Test
    void getMatchAttendances_MatchNotFound_ThrowsException() {

        when(matchRepository.existsById(matchId)).thenReturn(false);


        MatchException exception = assertThrows(MatchException.class, () ->
                attendanceService.getMatchAttendances(matchId));

        assertEquals("Match not found", exception.getMessage());
        assertEquals("MATCH_NOT_FOUND", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());


        verify(matchRepository).existsById(matchId);
        verify(attendanceRepository, never()).findByMatchId(any());
    }

    @Test
    void getPlayerAttendanceStatus_AttendanceExists_ReturnsStatus() {

        attendance.setStatus(AttendanceStatus.CONFIRMED);

        when(attendanceRepository.findByMatchIdAndPlayerId(matchId, playerId)).thenReturn(Optional.of(attendance));


        AttendanceStatus result = attendanceService.getPlayerAttendanceStatus(matchId, playerId);


        assertEquals(AttendanceStatus.CONFIRMED, result);


        verify(attendanceRepository).findByMatchIdAndPlayerId(matchId, playerId);
    }

    @Test
    void getPlayerAttendanceStatus_NoAttendance_ReturnsNotConfirmed() {

        when(attendanceRepository.findByMatchIdAndPlayerId(matchId, playerId)).thenReturn(Optional.empty());


        AttendanceStatus result = attendanceService.getPlayerAttendanceStatus(matchId, playerId);


        assertEquals(AttendanceStatus.NOT_CONFIRMED, result);


        verify(attendanceRepository).findByMatchIdAndPlayerId(matchId, playerId);
    }

    @Test
    void getAttendanceCount_Success() {

        long expectedCount = 5;

        when(attendanceRepository.countByMatchIdAndStatus(
                matchId, AttendanceStatus.CONFIRMED)).thenReturn(expectedCount);


        long result = attendanceService.getAttendanceCount(matchId);


        assertEquals(expectedCount, result);


        verify(attendanceRepository).countByMatchIdAndStatus(matchId, AttendanceStatus.CONFIRMED);
    }
}