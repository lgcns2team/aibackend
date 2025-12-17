package com.lgcns.haibackend.user.repository;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AIChatHistoryRepository {
    // RedisTemplate은 RefreshTokenRepository와 동일한 인스턴스를 사용합니다.
    private final RedisTemplate<String, String> redisTemplate; 
    private static final String HISTORY_KEY_PREFIX = "AI_HISTORY:";
    
    // [로그아웃 시 호출될 메서드] AI 대화 내역 삭제
    public void deleteHistory(UUID userId) {
        String userIdStr = userId.toString();
        redisTemplate.delete(HISTORY_KEY_PREFIX + userIdStr);
    }
}
