package com.lgcns.haibackend.aiPerson.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.lgcns.haibackend.aiPerson.service.AIPersonChatService;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ai-person")
@RequiredArgsConstructor
public class AIPersonChatController {

    private final AIPersonChatService aiPersonChatService;

    /**
     * AI 인물과의 채팅 (SSE 스트리밍)
     *
     * 예시 호출:
     * POST /api/ai-person/sejong/chat?userId=1
     * Body: { "message": "안녕하세요, 세종대왕님?" }
     */
    @PostMapping(
        value = "/{promptId}/chat",
        produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<String> chatWithPerson(
            @PathVariable("promptId") String promptId,
            @RequestParam("userId") Long userId,           // 우선은 쿼리 파라미터로 userId 전달
            @RequestBody AIPersonChatRequest request      // 프론트에서 오는 요청 바디
    ) {
        return aiPersonChatService.chat(promptId, userId, request.getMessage());
    }

    /**
     * 프론트 → 백엔드로 들어오는 요청 DTO
     * Bedrock용 ChatRequest(com.lgcns.haibackend.bedrock.client.ChatRequest)와
     * 이름이 겹치지 않도록 별도 이름 사용
     */
    @Data
    public static class AIPersonChatRequest {
        private String message;
    }
}
