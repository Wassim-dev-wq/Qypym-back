package org.fivy.matchservice.repository;

import org.fivy.matchservice.entity.MatchRequiredPosition;
import org.fivy.matchservice.entity.MatchRequiredPositionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MatchRequiredPositionRepository extends JpaRepository<MatchRequiredPosition, MatchRequiredPositionId> {
    List<MatchRequiredPosition> findByMatchId(UUID matchId);
}