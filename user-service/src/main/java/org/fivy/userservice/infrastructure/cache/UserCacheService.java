package org.fivy.userservice.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.userservice.api.dto.UserResponseDTO;
import org.jvnet.hk2.annotations.Service;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCacheService {
    private final RedisTemplate<String, UserResponseDTO> redisTemplate;
    private static final String USER_CACHE_PREFIX = "user:";

    public void cacheUser(UserResponseDTO user) {
        String key = USER_CACHE_PREFIX + user.getId();
        redisTemplate.opsForValue().set(key, user, Duration.ofHours(24));
    }

    public Optional<UserResponseDTO> getCachedUser(UUID userId) {
        String key = USER_CACHE_PREFIX + userId;
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }
}
