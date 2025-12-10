package com.lgcns.haibackend.bedrock.service;

import com.lgcns.haibackend.bedrock.client.*;
import com.lgcns.haibackend.common.redis.RedisChatRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
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

        // redis key관련 상수 및 key 생성 로직 추가
        private static final String CHATBOT_KEY_PREFIX = "chatbot:chat:";
        //private static final String DEFAULT_CHATBOT_ID = "kb-default"; // 일반 챗봇을 구분하는 ID
        
        private String getChatbotKey(Long userId) {
                return String.format("%s%d", CHATBOT_KEY_PREFIX, userId);
        }
        
        
        /**
         * Knowledge Base 검색 - 스트리밍
         * 이 메서드를 사용하세요!
         */
        public Flux<String> retrieveFromKnowledgeBase(String query, Long userId) {
                String redisKey = getChatbotKey(userId);
        
        log.info("[RAG] Starting RAG stream for UserID: {}", userId);

        // 1. 대화 기록(History) 불러오기 (Redis)
        // KnowledgeBaseRequest DTO에 History 필드가 있다면 여기서 History를 가져와야 합니다.
        List<Message> history = redisChatRepository.getMessages(redisKey);
        log.debug("[RAG] Loaded History Size: {}", history.size());
        // **[주의사항]** 현재 KnowledgeBaseRequest DTO에 History 필드가 없는 것으로 보입니다.
        // History를 RAG에 사용하려면 DTO를 수정해야 합니다. (이 예시에서는 History를 사용한다고 가정)

        // 2. 사용자 메시지를 Redis에 먼저 저장 (History Append - User Message)
        Message userMessage = Message.user(query);
        redisChatRepository.appendMessage(redisKey, userMessage); // Blocking I/O 발생 가능, 비동기 처리가 필요할 경우 Mono/Flux로 변경해야 함

        // 3. KnowledgeBaseRequest에 History 포함하여 요청 생성
        KnowledgeBaseRequest request = KnowledgeBaseRequest.builder()
                .query(query)
                .kbId(knowledgeBaseId)
                .modelArn(knowledgeBaseModelArn)
                .history(history) // DTO에 따라 주석 해제하거나 로직 추가
                .build();

        // 4. FastAPI 호출 (응답 스트림)
        return fastApiClient.retrieveFromKnowledgeBaseStream(request)
                .collect(Collectors.joining())
                // 5. 응답 완료 후 로직 수행 (History Append - AI Message)
                .doOnSuccess(fullResponse -> {
                    log.info("[RAG] Stream completed. Saving response to Redis.");
                    Message aiMessage = Message.assistant(fullResponse);
                    redisChatRepository.appendMessage(redisKey, aiMessage); // Blocking I/O
                })
                .doOnError(error -> {
                    log.error("[RAG ERROR] Stream failed for UserID: {}, Error: {}", userId, error.getMessage());
                })
                // 6. Mono<String>을 다시 Flux<String>으로 변환하여 스트리밍
                .flatMapMany(response -> {
                     return Flux.just(response); 
                });
    }
        

        /**
         * 사용 가능한 모델 목록 조회
         */
        public List<Model> getAvailableModels() {
                return fastApiClient.getModels();
        }

        /**
         * FastAPI 게이트웨이 상태 확인
         */
        public boolean isServiceAvailable() {
                return fastApiClient.isHealthy();
        }

        public Flux<String> chatWithHistory(String systemPrompt, List<Message> history) {

                ChatRequest request = ChatRequest.builder()
                        .model(chatModelName) // 사용할 Bedrock 모델 ID
                        .system(systemPrompt) // 시스템 프롬프트
                        .stream(true)         // SSE 스트리밍
                        .messages(history)    // 전체 대화 히스토리
                        .build();

                return fastApiClient.chatStream(request)
                        .doOnSubscribe(s ->
                                log.info("[AIPERSON CHAT] model={}, historySize={}", chatModelName, history.size()))
                        .doOnError(e ->
                                log.error("[AIPERSON CHAT ERROR] {}", e.getMessage()));
        }

        // BedrockService에 임시 구현
        // public Flux<String> chatWithHistory(String systemPrompt, List<Message> history) {
        //         log.info("[DUMMY CHAT] systemPrompt={}", systemPrompt);
        //         history.forEach(m -> log.info("history: {} - {}", m.getRole(), m.getContent()));

        //         // 테스트용 텍스트만 스트리밍
        //         return Flux.just("더미 응답입니다. systemPrompt 길이=" + systemPrompt.length());
        // }


}