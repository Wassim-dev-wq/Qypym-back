package org.fivy.matchservice.domain.repository;

import org.fivy.matchservice.domain.entity.UserPushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface UserPushTokenRepository extends JpaRepository<UserPushToken, UUID> {
    UserPushToken findByExpoToken(String expoToken);
    void deleteByUserIdAndExpoToken(UUID userId, String expoToken);
    List<UserPushToken> findByUserId(UUID userId);
}
