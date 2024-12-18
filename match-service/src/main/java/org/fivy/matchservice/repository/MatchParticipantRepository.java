package org.fivy.matchservice.repository;

import org.fivy.matchservice.entity.MatchParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, UUID> {
    List<MatchParticipant> findByMatchId(UUID matchId);
}