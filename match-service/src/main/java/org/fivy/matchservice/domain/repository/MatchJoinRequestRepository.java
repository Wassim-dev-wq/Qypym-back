package org.fivy.matchservice.domain.repository;

import org.fivy.matchservice.domain.entity.MatchJoinRequest;
import org.fivy.matchservice.domain.enums.JoinRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchJoinRequestRepository extends JpaRepository<MatchJoinRequest, UUID> {

    boolean existsByMatchIdAndUserIdAndRequestStatusIn(
            UUID matchId,
            UUID userId,
            List<JoinRequestStatus> statuses
    );

    long countByMatchIdAndRequestStatus(UUID matchId, JoinRequestStatus status);
}
