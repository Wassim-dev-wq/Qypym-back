package org.fivy.matchservice.domain.repository;

import org.fivy.matchservice.domain.entity.MatchPlayer;
import org.fivy.matchservice.domain.enums.MatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchPlayerRepository extends JpaRepository<MatchPlayer, UUID> {
    List<MatchPlayer> findByMatchId(UUID matchId);

    boolean existsByMatchIdAndPlayerId(UUID matchId, UUID currentUserId);

    int countByMatchId(UUID matchId);

    List<MatchPlayer> findAllByPlayerId(UUID currentUserId);

    List<UUID> findPlayerIdsByMatchId(UUID id);

    Optional<MatchPlayer> findByMatchIdAndPlayerId(UUID matchId, UUID playerId);

    List<MatchPlayer> findAllByMatchId(UUID id);

    @Query("SELECT mp FROM MatchPlayer mp JOIN FETCH mp.match m WHERE mp.playerId = :playerId ORDER BY m.startDate DESC")
    Page<MatchPlayer> findByPlayerIdOrderByMatchStartDateDesc(UUID playerId, Pageable pageable);

    List<MatchPlayer> findByTeamId(UUID teamId);

    Page<MatchPlayer> findByPlayerIdAndMatchStatusOrderByMatchStartDateDesc(UUID userId, MatchStatus completed, Pageable pageable);

    Optional<MatchPlayer> findByPlayerId(UUID playerId);

    void deleteByMatchId(UUID matchId);
}