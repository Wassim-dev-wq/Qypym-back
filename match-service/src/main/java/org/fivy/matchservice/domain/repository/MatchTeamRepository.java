package org.fivy.matchservice.domain.repository;

import org.fivy.matchservice.domain.entity.MatchTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchTeamRepository extends JpaRepository<MatchTeam, UUID> {
    List<MatchTeam> findByMatchId(UUID matchId);
}