package com.lgcns.haibackend.aiPerson.redis;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcns.haibackend.bedrock.client.Message;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RedisChatRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration DEFAULT_TTL = Duration.ofHours(6);

    // 채팅 히스토리 전체 조회: List<Message>
    public List<Message> getMessages(String key) {
        List<String> rawList = redisTemplate.opsForList().range(key, 0, -1);
        if (rawList == null || rawList.isEmpty()) {
            return new ArrayList<>();
        }

        return rawList.stream()
                .map(this::deserialize)
                .collect(Collectors.toList());
    }

    // 메시지 1개 추가
    public void appendMessage(String key, Message message) {
        String json = serialize(message);
        redisTemplate.opsForList().rightPush(key, json);
        // TTL 갱신 (로그아웃으로 지우더라도, 혹시 로그아웃 안하고 오래 방치되는 경우를 위해)
        redisTemplate.expire(key, DEFAULT_TTL);
    }

    // 특정 key의 히스토리 삭제
    public void deleteHistory(String key) {
        redisTemplate.delete(key);
    }

    // userId 기준으로 모든 AIPerson 히스토리 삭제
    public void deleteAllByUserId(Long userId) {
        String pattern = "aiperson:chat:*:" + userId;
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private String serialize(Message message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 직렬화 실패", e);
        }
    }

    private Message deserialize(String json) {
        try {
            return objectMapper.readValue(json, Message.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 역직렬화 실패", e);
        }
    }
}
