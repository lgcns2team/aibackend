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
}