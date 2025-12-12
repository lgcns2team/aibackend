package com.lgcns.haibackend.bedrock.controller;

import com.lgcns.haibackend.bedrock.service.BedrockService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Bedrock AI API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class BedrockController {

    private final BedrockService bedrockService;

    /**
     * AI 채팅 엔드포인트 (스트리밍) - Knowledge Base 사용
     * 프론트엔드에서 /api/ai/chat 호출 시 사용됨
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public Flux<String> chat(HttpServletRequest request, @RequestBody ChatInput input) {
            // 💡 [추가] JWT 필터가 저장한 userIdStr과 role 추출
        String userIdStr = (String) request.getAttribute("userIdStr");
        String role = (String) request.getAttribute("role");
        
        if (userIdStr == null) {
            log.error("JWT 토큰 없음: userIdStr is null");
            return Flux.error(new RuntimeException("인증 정보가 유효하지 않습니다."));
        }
        
        log.info("===========================================");
        //log.info("[CHAT REQUEST] UserID: {}, Query: {}", input.getUserId(), input.getMessage());
        log.info("[CHAT REQUEST] UserID (from JWT): {}, Role: {}, Query: {}", userIdStr, role, input.getMessage());
        log.info("===========================================");

        return bedrockService.retrieveFromKnowledgeBase(input.getMessage(), userIdStr)
                .doOnSubscribe(subscription -> {
                    log.info("[KB SEARCH] Starting Knowledge Base search...");
                })
                .doOnSubscribe(subscription -> {
                    log.info("[KB SEARCH] Starting Knowledge Base search...");
                })
                .map(chunk -> {
                    // JSON 래핑: {"type": "content", "text": "..."}
                    // Spring이 자동으로 "data: " prefix를 붙이므로 여기서는 제거
                    String escapedChunk = chunk.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
                    return "{\"type\": \"content\", \"text\": \"" + escapedChunk + "\"}";
                })
                .doOnNext(chunk -> {
                    // 스트리밍 청크마다 로그
                    log.info("[SENDING CHUNK] {}", chunk.substring(0, Math.min(100, chunk.length())));
                })
                .doOnComplete(() -> {
                    log.info("[KB SEARCH] Knowledge Base search completed successfully");
                    log.info("===========================================");
                })
                .doOnError(error -> {
                    log.error("[KB SEARCH ERROR] {}", error.getMessage());
                    log.error("===========================================", error);
                })
                .onErrorResume(error -> {
                    log.error("Error in chat: {}", error.getMessage());
                    return Flux.just("data: Error: " + error.getMessage() + "\n\n");
                });
    }


    /**
     * 헬스 체크
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.info("[HEALTH] Checking service health");

        boolean isHealthy = bedrockService.isServiceAvailable();

        log.info("[HEALTH] Status: {}", isHealthy ? "healthy" : "unhealthy");
        return ResponseEntity.ok(Map.of(
                "status", isHealthy ? "healthy" : "unhealthy",
                "fastapi_gateway", isHealthy ? "connected" : "disconnected"));
    }

    // ===== DTO 클래스들 =====

    @Data
    public static class ChatInput {
        // private Long userId;
        private String message;
    }
}