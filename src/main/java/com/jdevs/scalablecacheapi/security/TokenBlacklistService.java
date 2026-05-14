package com.jdevs.scalablecacheapi.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "blacklisted_access_token:";

    private final StringRedisTemplate stringRedisTemplate;

    public void blacklistToken(String token, long ttlMillis) {
        if (ttlMillis <= 0) {
            return;
        }

        String key = BLACKLIST_PREFIX + token;

        stringRedisTemplate.opsForValue().set(
                key,
                "revoked",
                Duration.ofMillis(ttlMillis)
        );
    }

    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;

        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }
}
