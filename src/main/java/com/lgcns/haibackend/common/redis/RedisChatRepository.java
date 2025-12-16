package com.lgcns.haibackend.common.redis;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcns.haibackend.bedrock.domain.dto.MessageDTO;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RedisChatRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration DEFAULT_TTL = Duration.ofHours(6);

    // key에 해당하는 전체 메시지 히스토리 조회
    public List<MessageDTO> getMessages(String key) {
        List<String> rawList = redisTemplate.opsForList().range(key, 0, -1);
        if (rawList == null || rawList.isEmpty()) {
            return new ArrayList<>();
        }

        return rawList.stream()
                .map(this::deserialize)
                .collect(Collectors.toList());
    }

    // 메시지 1개 추가
    public void appendMessage(String key, MessageDTO message) {
        appendMessage(key, message, DEFAULT_TTL);
    }

    // 메시지 1개 추가 (TTL 지정)
    public void appendMessage(String key, MessageDTO message, Duration ttl) {
        String json = serialize(message);
        redisTemplate.opsForList().rightPush(key, json);
        if (ttl != null) {
            redisTemplate.expire(key, ttl);
        }
    }

    // 특정 key의 히스토리 삭제
    public void deleteByKey(String key) {
        redisTemplate.delete(key);
    }

    // 패턴으로 여러 키 삭제
    // pattern = aiperson:chat:*:1
    public void deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public void deleteAllAIPersonChats(UUID userId) {
        // aiperson:chat:*:userId 패턴으로 모든 키 검색
        String pattern = "aiperson:chat:*:" + userId;
        Set<String> keys = redisTemplate.keys(pattern);
        
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            System.out.println("[REDIS] Deleted " + keys.size() + " AI Person chat keys for user: " + userId);
        }
    }

    private String serialize(MessageDTO message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 직렬화 실패", e);
        }
    }

    private MessageDTO deserialize(String json) {
        try {
            return objectMapper.readValue(json, MessageDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 역직렬화 실패", e);
        }
    }
}
