package com.lgcns.haibackend.aiPerson.controller;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.lgcns.haibackend.aiPerson.service.AIPersonChatService;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/api/ai-person")
@RequiredArgsConstructor
public class AIPersonChatController {

    private final AIPersonChatService aiPersonChatService;

    /**
     * AI 인물 채팅 엔드포인트 (스트리밍)
     */
    @PostMapping(value = "/{promptId}/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatWithPerson(
            @PathVariable("promptId") String promptId,
            @RequestBody AIPersonChatRequest request) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString(authentication.getPrincipal().toString());

        log.info("===========================================");
        log.info("[AI PERSON CHAT REQUEST] PromptID: {}, UserID: {}, Message: {}", 
                promptId, userId, request.getMessage());
        log.info("===========================================");

        return aiPersonChatService.chat(promptId, userId, request.getMessage())
                .doOnSubscribe(subscription -> {
                    log.info("[AI PERSON CHAT] Starting chat with AI person...");
                })
                .map(chunk -> {
                    // JSON 래핑: {"type": "content", "text": "..."}
                    String escapedChunk = chunk.replace("\\", "\\\\")
                                              .replace("\"", "\\\"")
                                              .replace("\n", "\\n");
                    return "{\"type\": \"content\", \"text\": \"" + escapedChunk + "\"}";
                })
                .doOnNext(chunk -> {
                    // 스트리밍 청크마다 로그
                    log.info("[SENDING CHUNK] {}", chunk.substring(0, Math.min(100, chunk.length())));
                })
                .doOnComplete(() -> {
                    log.info("[AI PERSON CHAT] Chat completed successfully");
                    log.info("===========================================");
                })
                .doOnError(error -> {
                    log.error("[AI PERSON CHAT ERROR] {}", error.getMessage());
                    log.error("===========================================", error);
                })
                .onErrorResume(error -> {
                    log.error("Error in AI person chat: {}", error.getMessage());
                    return Flux.just("data: Error: " + error.getMessage() + "\n\n");
                });
    }

    // AI Person ChatRequest DTO
    @Data
    public static class AIPersonChatRequest {
        private String message;
    }
}