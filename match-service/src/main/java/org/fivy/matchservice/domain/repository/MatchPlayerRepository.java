package org.fivy.matchservice.domain.repository;

import org.fivy.matchservice.domain.entity.MatchPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchPlayerRepository extends JpaRepository<MatchPlayer, UUID> {
    List<MatchPlayer> findByMatchId(UUID matchId);

    boolean existsByMatchIdAndPlayerId(UUID matchId, UUID currentUserId);

    int countByMatchId(UUID matchId);

    List<MatchPlayer> findAllByPlayerId(UUID currentUserId);
}