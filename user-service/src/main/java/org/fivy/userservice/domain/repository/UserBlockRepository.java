package org.fivy.userservice.domain.repository;

import org.fivy.userservice.domain.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {
    List<UserBlock> findByBlockerId(UUID blockerId);
    List<UserBlock> findByBlockedId(UUID blockedId);
    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);
    void deleteByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);
}