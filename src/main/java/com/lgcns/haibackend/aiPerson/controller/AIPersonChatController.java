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

    // AI 인물 채팅
    @PostMapping(
        value = "/{promptId}/chat",
        produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<String> chatWithPerson(
            @PathVariable("promptId") String promptId,
            @RequestParam("userId") Long userId,
            @RequestBody AIPersonChatRequest request
    ) {
        return aiPersonChatService.chat(promptId, userId, request.getMessage());
    }

    // Bedrock용 ChatRequest
    @Data
    public static class AIPersonChatRequest {
        private String message;
    }
}
