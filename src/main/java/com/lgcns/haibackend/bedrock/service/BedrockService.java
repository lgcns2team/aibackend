package com.lgcns.haibackend.bedrock.service;

import com.lgcns.haibackend.aiPerson.domain.dto.PromptRequest;
import com.lgcns.haibackend.bedrock.client.*;
import com.lgcns.haibackend.bedrock.domain.dto.KnowledgeBaseRequest;
import com.lgcns.haibackend.bedrock.domain.dto.MessageDTO;
import com.lgcns.haibackend.common.redis.RedisChatRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Bedrock AI 서비스
 * FastAPI 게이트웨이를 통해 Claude AI와 통신
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BedrockService {
        
        private final RedisChatRepository redisChatRepository;
        private final FastApiClient fastApiClient;

        @Value("${aws.bedrock.knowledge-base.id}")
        private String knowledgeBaseId;

        @Value("${aws.bedrock.knowledge-base.model-arn}")
        private String knowledgeBaseModelArn;

        @Value("${aws.bedrock.model.chat}")
        private String chatModelName;

        private String getChatbotKey(UUID userId) {
                return "chatbot:chat:" + userId;
        }

        /**
         * Knowledge Base 검색 - 스트리밍
         */
        public Flux<String> retrieveFromKnowledgeBase(String query, UUID userId) {
                String redisKey = getChatbotKey(userId);

                MessageDTO userMessage = MessageDTO.user(query);
                redisChatRepository.appendMessage(redisKey, userMessage);

                KnowledgeBaseRequest request = KnowledgeBaseRequest.builder()
                                .query(query)
                                .kbId(knowledgeBaseId)
                                .modelArn(knowledgeBaseModelArn)
                                .build();

                return fastApiClient.retrieveFromKnowledgeBaseStream(request)
                                .collect(Collectors.joining())
                                .doOnSuccess(fullResponse -> {
                                        log.info("[RAG] Stream completed. Saving response to Redis.");
                                        MessageDTO aiMessage = MessageDTO.assistant(fullResponse);
                                        redisChatRepository.appendMessage(redisKey, aiMessage);
                                })
                                .doOnError(error -> {
                                        log.error("[RAG ERROR] Stream failed for UserID: {}, Error: {}", userId,
                                                        error.getMessage());
                                })
                                .flatMapMany(response -> {
                                        return Flux.just(response);
                                });
        }

        /**
         * FastAPI 게이트웨이 상태 확인
         */
        public boolean isServiceAvailable() {
                return fastApiClient.isHealthy();
        }

        /**
         * Bedrock Prompt 기반 채팅 - 변수 없이 (기존 호환성 유지)
         */
        public Flux<String> chatWithPrompt(String promptId, String userQuery) {
                return chatWithPrompt(promptId, userQuery, null);
        }

        /**
         * Bedrock Prompt 기반 채팅 - 변수 포함 ⭐ 새로 추가
         * 
         * @param promptId 프롬프트 ID
         * @param userQuery 사용자 질문
         * @param variables 프롬프트 변수 (name, era, summary 등)
         * @return 스트리밍 응답
         */
        public Flux<String> chatWithPrompt(String promptId, String userQuery, Map<String, String> variables) {
                log.info("🚀 [AIPERSON PROMPT CHAT START] promptId={}, query={}", promptId, userQuery);
                
                if (variables != null && !variables.isEmpty()) {
                        log.info("📋 [AIPERSON VARIABLES] {}", variables);
                }

                // PromptRequest 생성
                PromptRequest.PromptRequestBuilder builder = PromptRequest.builder()
                                .promptId(promptId)
                                .userQuery(userQuery);
                
                // 변수가 있으면 추가
                if (variables != null && !variables.isEmpty()) {
                        builder.variables(variables);
                } else {
                        builder.variables(new HashMap<>());
                }

                PromptRequest request = builder.build();

                return fastApiClient.chatPromptStream(request)
                                .doOnNext(chunk -> {
                                        log.debug("📦 [AIPERSON CHUNK RECEIVED] length={}, preview={}", 
                                                chunk.length(), 
                                                chunk.substring(0, Math.min(50, chunk.length())));
                                })
                                .doOnComplete(() -> {
                                        log.info("✅ [AIPERSON PROMPT CHAT COMPLETE]");
                                })
                                .doOnError(e -> {
                                        log.error("❌ [AIPERSON PROMPT CHAT ERROR] promptId={}, error={}", 
                                                promptId, e.getMessage(), e);
                                })
                                .doOnSubscribe(s -> {
                                        log.info("🔗 [AIPERSON PROMPT CHAT SUBSCRIBED] Starting stream...");
                                });
        }
}