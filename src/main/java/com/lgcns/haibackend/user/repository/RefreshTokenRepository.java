package com.lgcns.haibackend.user.repository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private final RedisTemplate<String, String> redisTemplate;

    public void save(UUID userId, String refreshToken, long expirationMs) {
        String userIdStr = userId.toString();
        redisTemplate.opsForValue().set(
                "RT:" + userIdStr,
                refreshToken,
                Duration.ofMillis(expirationMs));
    }

    public String findByUserId(UUID userId) {
        String userIdStr = userId.toString();
        return redisTemplate.opsForValue().get("RT:" + userIdStr);
    }

    public void delete(UUID userId) {
        String userIdStr = userId.toString();
        redisTemplate.delete("RT:" + userIdStr);
    }
}
