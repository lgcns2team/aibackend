package com.lgcns.haibackend.aiPerson.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.lgcns.haibackend.aiPerson.domain.dto.AIPersonDetailDTO;
import com.lgcns.haibackend.aiPerson.redis.RedisChatRepository;
import com.lgcns.haibackend.bedrock.client.Message;
import com.lgcns.haibackend.bedrock.service.BedrockService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class AIPersonChatService {

    private final AIPersonService aiPersonService;
    private final BedrockService bedrockService;
    private final RedisChatRepository redisChatRepository;

    public Flux<String> chat(String promptId, Long userId, String userMessage) {

        // 1) 인물 상세 정보 불러오기 (UI + 프롬프트 생성에 사용)
        AIPersonDetailDTO person = aiPersonService.getPersonDetail(promptId);

        // 2) Redis에서 과거 히스토리 조회
        String historyKey = buildHistoryKey(promptId, userId);
        List<Message> history = redisChatRepository.getMessages(historyKey);

        // 3) 이번 user 메시지 추가
        Message userMsg = Message.user(userMessage);   // Message.user(...)는 앞에서 추가한 static 팩토리라고 가정
        history.add(userMsg);

        // 4) DTO 정보를 바탕으로 systemPrompt 생성
        String systemPrompt = buildSystemPrompt(person);

        // 5) Bedrock에 넘길 요청 (히스토리 기반)
        Flux<String> stream = bedrockService.chatWithHistory(
                systemPrompt,   // 생성한 시스템 프롬프트
                history         // 지금까지의 전체 대화
        );

        StringBuilder assistantBuffer = new StringBuilder();

        // 6) 스트리밍 응답을 그대로 흘려보내면서, 최종 답변은 Redis에 저장
        return stream
                .doOnNext(chunk -> {
                    // chunk 는 FastApiClient에서 이미 텍스트만 뽑아준 String이라고 가정
                    assistantBuffer.append(chunk);
                })
                .doOnComplete(() -> {
                    // 7) 최종 assistant 메시지를 Redis에 저장
                    Message assistantMsg = Message.assistant(assistantBuffer.toString());
                    redisChatRepository.appendMessage(historyKey, userMsg);
                    redisChatRepository.appendMessage(historyKey, assistantMsg);
                });
    }

    private String buildHistoryKey(String promptId, Long userId) {
        return "aiperson:chat:" + promptId + ":" + userId;
    }

    /**
     * AIPersonDetailDTO 정보(name, summary, greetingMessage 등)를 바탕으로
     * Bedrock에 넘길 system 프롬프트를 생성
     */
    private String buildSystemPrompt(AIPersonDetailDTO person) {
        StringBuilder sb = new StringBuilder();

        // 인물 역할 정의
        sb.append("당신은 '")
          .append(person.getName())
          .append("' 역할을 맡은 AI입니다. ");

        // 인물 요약 정보
        if (person.getSummary() != null && !person.getSummary().isBlank()) {
            sb.append("다음은 당신에 대한 요약 정보입니다: ")
              .append(person.getSummary())
              .append(" ");
        }

        // 말투/톤 등 기본 안내 (필요에 따라 수정 가능)
        sb.append("사용자의 질문에 대해, 실제 ")
          .append(person.getName())
          .append("라면 대답할 법한 방식으로, 친절하고 이해하기 쉽게 한국어로 설명하세요. ")
          .append("역사적 사실이나 맥락이 애매한 경우에는 추측이라고 분명히 밝혀주세요.");

        // greetingMessage 는 채팅창 첫 메시지/프론트에서 주로 쓰일 테니,
        // system prompt에 굳이 넣지 않아도 됨. 넣고 싶으면 아래처럼 추가 가능:
        /*
        if (person.getGreetingMessage() != null && !person.getGreetingMessage().isBlank()) {
            sb.append(" 대화를 시작할 때는 다음과 같은 분위기로 응답을 시작해도 좋습니다: ")
              .append(person.getGreetingMessage());
        }
        */

        return sb.toString();
    }
}
