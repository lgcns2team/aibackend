package com.lgcns.haibackend.aiPerson.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.lgcns.haibackend.aiPerson.domain.dto.AIPersonDetailDTO;
import com.lgcns.haibackend.bedrock.domain.dto.MessageDTO;
import com.lgcns.haibackend.bedrock.service.BedrockService;
import com.lgcns.haibackend.common.redis.RedisChatRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIPersonChatService {

    private final BedrockService bedrockService;
    private final RedisChatRepository redisChatRepository;
    private final AIPersonService aiPersonService;

    @Value("${aws.bedrock.prompt.ai-person}")
    private String aiPersonPromptId;

    public Flux<String> chat(String promptId, UUID userId, String userMessage) {

        log.info("[AI PERSON CHAT] Starting chat with AI person...");
        // 1) 인물 상세 정보 불러오기
        AIPersonDetailDTO person = aiPersonService.getPersonDetail(promptId);

        log.info("Chat with AI Person: name={}, era={}", person.getName(), person.getEra());

        // 2) Redis 히스토리 키
        String historyKey = buildAIPersonKey(promptId, userId);

        // 3) user 메시지 (Redis 저장용)
        MessageDTO userMsg = MessageDTO.user(userMessage);

        // 4) Bedrock Prompt에 넘길 변수 맵핑
        Map<String, String> variables = new HashMap<>();
        variables.put("name", person.getName());
        variables.put("era", person.getEra() != null ? person.getEra() : "알 수 없음");
        variables.put("summary", person.getSummary() != null ? person.getSummary() : "정보 없음");

        log.info("Bedrock variables: {}", variables);

        // 5) Bedrock Prompt API 호출 (promptId와 변수 전달)
        Flux<String> stream = bedrockService.chatWithPrompt(aiPersonPromptId, userMessage, variables);

        StringBuilder assistantBuffer = new StringBuilder();

        // 6) 스트리밍 응답 처리 및 Redis 저장
        return stream
                .doOnNext(chunk -> {
                    log.debug("Received chunk: {}", chunk);
                    assistantBuffer.append(chunk);
                })
                .doOnComplete(() -> {
                    String fullResponse = assistantBuffer.toString();
                    log.info("Complete response length: {}", fullResponse.length());

                    // Redis에 저장
                    MessageDTO assistantMsg = MessageDTO.assistant(fullResponse);
                    redisChatRepository.appendMessage(historyKey, userMsg);
                    redisChatRepository.appendMessage(historyKey, assistantMsg);

                    log.info("Saved chat history for key: {}", historyKey);
                })
                .doOnError(error -> {
                    log.error("Error during chat stream: ", error);
                });
    }

    private String buildAIPersonKey(String promptId, UUID userId) {
        return String.format("aiperson:chat:%s:%s", promptId, userId);
    }
}