package org.fivy.matchservice.domain.repository;

import org.fivy.matchservice.domain.entity.SavedMatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavedMatchRepository extends JpaRepository<SavedMatch, UUID> {
    Page<SavedMatch> findByUserId(UUID userId, Pageable pageable);
    Optional<SavedMatch> findByMatchIdAndUserId(UUID matchId, UUID userId);
    boolean existsByMatchIdAndUserId(UUID matchId, UUID userId);
    void deleteByMatchIdAndUserId(UUID matchId, UUID userId);

    int countByMatchId(UUID matchId);
}