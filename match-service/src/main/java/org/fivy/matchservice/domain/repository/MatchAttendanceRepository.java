package org.fivy.matchservice.domain.repository;

import org.fivy.matchservice.domain.entity.MatchAttendance;
import org.fivy.matchservice.domain.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchAttendanceRepository extends JpaRepository<MatchAttendance, UUID> {
    Optional<MatchAttendance> findByMatchIdAndPlayerId(UUID matchId, UUID playerId);
    List<MatchAttendance> findByMatchId(UUID matchId);
    long countByMatchIdAndStatus(UUID matchId, AttendanceStatus status);
    boolean existsByMatchIdAndPlayerIdAndStatus(UUID matchId, UUID playerId, AttendanceStatus status);
    List<MatchAttendance> findByPlayerId(UUID playerId);
}