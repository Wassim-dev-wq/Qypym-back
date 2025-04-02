package org.fivy.matchservice.infrastructure.config.kafka.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import org.fivy.matchservice.api.dto.response.MatchResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MatchCache {
    private static final String MATCH_CACHE_PREFIX = "match:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, MatchResponse> redisTemplate;

    public void cacheMatch(MatchResponse match) {
        String cacheKey = buildKey(match.getId());
        redisTemplate.opsForValue().set(cacheKey, match, CACHE_TTL);
    }

    public MatchResponse getMatch(UUID matchId) {
        String cacheKey = buildKey(matchId);
        return redisTemplate.opsForValue().get(cacheKey);
    }

    public void evictMatch(UUID matchId) {
        String cacheKey = buildKey(matchId);
        redisTemplate.delete(cacheKey);
    }

    private String buildKey(UUID matchId) {
        return MATCH_CACHE_PREFIX + matchId.toString();
    }
}
