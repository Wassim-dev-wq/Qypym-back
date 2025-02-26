package org.fivy.userservice.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fivy.userservice.api.dto.UserResponseDTO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCacheService {
    private static final String USER_CACHE_PREFIX = "user:";
    private final RedisTemplate<String, UserResponseDTO> redisTemplate;

    public void cacheUser(UserResponseDTO user) {
        String key = USER_CACHE_PREFIX + user.getId();
        redisTemplate.opsForValue().set(key, user, Duration.ofHours(24));
    }

    public Optional<UserResponseDTO> getCachedUser(UUID userId) {
        String key = USER_CACHE_PREFIX + userId;
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }
}
