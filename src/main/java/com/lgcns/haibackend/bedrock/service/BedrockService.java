package com.lgcns.haibackend.bedrock.service;

import com.lgcns.haibackend.bedrock.client.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Bedrock AI 서비스
 * FastAPI 게이트웨이를 통해 Claude AI와 통신
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BedrockService {

        @Value("${aws.bedrock.knowledge-base.id}")
        private String knowledgeBaseId;

        @Value("${aws.bedrock.knowledge-base.model-arn}")
        private String knowledgeBaseModelArn;

        @Value("${aws.bedrock.model.chat}")
        private String chatModelName;

        private final FastApiClient fastApiClient;

        /**
         * Knowledge Base 검색 - 스트리밍
         * ✅ 이 메서드를 사용하세요!
         */
        public Flux<String> retrieveFromKnowledgeBase(String query) {
                log.info("Searching Knowledge Base with query: {}", query);
                log.debug("Using KB ID: {}, Model ARN: {}", knowledgeBaseId, knowledgeBaseModelArn);

                KnowledgeBaseRequest request = KnowledgeBaseRequest.builder()
                                .query(query)
                                .kbId(knowledgeBaseId)
                                .modelArn(knowledgeBaseModelArn)
                                .build();

                return fastApiClient.retrieveFromKnowledgeBaseStream(request)
                                .doOnComplete(() -> log.info("KB search completed"))
                                .doOnError(error -> log.error("KB search failed: {}", error.getMessage()));
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