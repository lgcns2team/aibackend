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

import java.util.List;
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
        // redischatrepository 주입
        private final RedisChatRepository redisChatRepository;

        @Value("${aws.bedrock.knowledge-base.id}")
        private String knowledgeBaseId;

        @Value("${aws.bedrock.knowledge-base.model-arn}")
        private String knowledgeBaseModelArn;

        @Value("${aws.bedrock.model.chat}")
        private String chatModelName;

        private final FastApiClient fastApiClient;

        // // redis key관련 상수 및 key 생성 로직 추가
        // private static final String CHATBOT_KEY_PREFIX = "chatbot:chat:";

        private String getChatbotKey(UUID userId) {
                return "chatbot:chat:" + userId;
        }

        /**
         * Knowledge Base 검색 - 스트리밍
         * 이 메서드를 사용하세요!
         */
        public Flux<String> retrieveFromKnowledgeBase(String query, UUID userId) {
                String redisKey = getChatbotKey(userId);

                // log.info("[RAG] Starting RAG stream for UserID: {}", userId);

                // 1. 대화 기록(History) 불러오기 (Redis)
                // List<Message> history = redisChatRepository.getMessages(redisKey);
                // log.debug("[RAG] Loaded History Size: {}", history.size());

                // 2. 사용자 메시지를 Redis에 먼저 저장 (History Append - User Message)
                MessageDTO userMessage = MessageDTO.user(query);
                redisChatRepository.appendMessage(redisKey, userMessage);
                // ----------------------------------------------------
                // [수정 시작] 실제 AI 호출 로직을 Mocking으로 대체 
                // ----------------------------------------------------
                // String mockResponse = "AI 테스트 응답입니다. 요청: '" + query + "'. 시간: " + System.currentTimeMillis();
                // // 2. 가짜 응답을 Mono<String>으로 래핑하고, 성공 핸들러(doOnSuccess)는 유지합니다.
                // return Flux.just(mockResponse)
                //         .collect(Collectors.joining()) // Mono<String>으로 변환
                        
                //         // 3. 응답 완료 후 로직 수행 (History Append - AI Message)
                //         .doOnSuccess(fullResponse -> {
                //                 log.info("[RAG] Mock Stream completed. Saving response to Redis.");
                //                 MessageDTO aiMessage = MessageDTO.assistant(fullResponse);
                //                 redisChatRepository.appendMessage(redisKey, aiMessage);
                //         })
                //         .doOnError(error -> {
                //                 // Mocking이므로 에러가 발생할 일은 거의 없지만, 로직은 유지
                //                 log.error("[RAG ERROR] Mock failed for UserID: {}", userIdStr);
                //         })
                        
                //         // 4. Mono<String>을 다시 Flux<String>으로 변환하여 반환
                //         .flatMapMany(response -> {
                //                 return Flux.just(response);
                //         });

                // 3. KnowledgeBaseRequest에 History 포함하여 요청 생성
                KnowledgeBaseRequest request = KnowledgeBaseRequest.builder()
                                .query(query)
                                .kbId(knowledgeBaseId)
                                .modelArn(knowledgeBaseModelArn)
                                //.history(history)
                                .build();

                // 4. FastAPI 호출 (응답 스트림)
                return fastApiClient.retrieveFromKnowledgeBaseStream(request)
                                .collect(Collectors.joining())
                                // 5. 응답 완료 후 로직 수행 (History Append - AI Message)
                                .doOnSuccess(fullResponse -> {
                                        log.info("[RAG] Stream completed. Saving response to Redis.");
                                        MessageDTO aiMessage = MessageDTO.assistant(fullResponse);
                                        redisChatRepository.appendMessage(redisKey, aiMessage);
                                })
                                .doOnError(error -> {
                                        log.error("[RAG ERROR] Stream failed for UserID: {}, Error: {}", userId,
                                                        error.getMessage());
                                })
                                // 6. Mono<String>을 다시 Flux<String>으로 변환하여 스트리밍
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
         * Bedrock Prompt (프롬프트 관리 기능) 기반 채팅
         * ✅ 수정: 실시간 스트리밍 지원 + 상세 로그 추가
         */
        public Flux<String> chatWithPrompt(String promptId, String userQuery) {
                log.info("🚀 [AIPERSON PROMPT CHAT START] promptId={}, query={}", promptId, userQuery);
                
                PromptRequest request = PromptRequest.builder()
                                .promptId(promptId)
                                .userQuery(userQuery)
                                .build();

                return fastApiClient.chatPromptStream(request)
                                // ✅ 각 청크를 실시간으로 전달 (collect 제거)
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