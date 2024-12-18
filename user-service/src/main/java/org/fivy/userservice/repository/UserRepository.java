package org.fivy.userservice.repository;

import org.fivy.userservice.entity.User;
import org.fivy.userservice.enums.UserStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :userId")
    void updateUserStatus(UUID userId, UserStatus status);

    Optional<User> findByKeycloakUserId(String keycloakUserId);
    boolean existsByKeycloakUserId(String keycloakUserId);
}
