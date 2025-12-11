package com.lgcns.haibackend.aiPerson.service;

import java.util.List;

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

    private final AIPersonService aiPersonService;
    private final BedrockService bedrockService;
    private final RedisChatRepository redisChatRepository;

    public Flux<String> chat(String promptId, Long userId, String userMessage) {

        // 1) 인물 상세 정보 불러오기
        // AIPersonDetailDTO person = aiPersonService.getPersonDetail(promptId);

        // 2) Redis에서 과거 히스토리 조회 (저장은 하지만, Prompt API로 보낼 방법이 명확하지 않아 일단 조회만 함)
        String historyKey = buildAIPersonKey(promptId, userId);
        // List<Message> history = redisChatRepository.getMessages(historyKey);

        // 3) user 메시지 (Redis 저장을 위해 객체 생성)
        MessageDTO userMsg = MessageDTO.user(userMessage);

        // 4) Bedrock Prompt에 넘길 변수 맵핑
        // (Prompt 템플릿에서 {{name}}, {{era}} 등을 사용할 수 있도록 전달)
        // java.util.Map<String, Object> variables = new java.util.HashMap<>();
        // variables.put("name", person.getName());
        // variables.put("era", person.getEra() != null ? person.getEra() : "");
        // variables.put("summary", person.getSummary() != null ? person.getSummary() : "");
        // variables.put("greeting", person.getGreetingMessage() != null ? person.getGreetingMessage() : "");

        // 5) Bedrock Prompt API 호출
        // Prompt ID는 URL의 promptId를 그대로 사용 (DB의 promptId가 Bedrock Prompt ID와 일치한다고 가정)
        Flux<String> stream = bedrockService.chatWithPrompt(promptId, userMessage);

        StringBuilder assistantBuffer = new StringBuilder();

        // 6) 스트리밍 응답을 그대로 흘려보내면서, 최종 답변은 Redis에 저장
        return stream
                .doOnNext(chunk -> {
                    assistantBuffer.append(chunk);
                })
                .doOnComplete(() -> {
                    // 7) 이번 대화(질문/답변)를 Redis에 저장
                    MessageDTO assistantMsg = MessageDTO.assistant(assistantBuffer.toString());
                    redisChatRepository.appendMessage(historyKey, userMsg);
                    redisChatRepository.appendMessage(historyKey, assistantMsg);
                });
    }

    private String buildAIPersonKey(String promptId, Long userId) {
        return "aiperson:chat:" + promptId + ":" + userId;
    }

//     // AIPersonDetailDTO 정보(name, summary, greetingMessage 등)를 바탕으로 Bedrock에 넘길
//     // system 프롬프트를 생성
//     private String buildSystemPrompt(AIPersonDetailDTO person) {
//         StringBuilder sb = new StringBuilder();

//         // 1. 역할/정체성 - 1인칭 강제
//         sb.append("# 역할 설정\n")
//                 .append("당신은 **")
//                 .append(person.getName())
//                 .append("** 그 자체입니다. 다른 학자나 해설자가 아니라, 실제 역사 속의 ")
//                 .append(person.getName())
//                 .append(" 본인으로서만 말해야 합니다.\n")
//                 .append("항상 1인칭(“나”, “내가”, “나는”, “저는”)을 사용하여 이야기하고, ")
//                 .append("절대로 \"")
//                 .append(person.getName())
//                 .append("은(는)~\" 처럼 자신을 3인칭으로 부르지 마세요.\n\n");

//         // 2. 생애/업적 요약
//         if (person.getSummary() != null && !person.getSummary().isBlank()) {
//             sb.append("# 당신의 생애와 업적 요약\n")
//                     .append(person.getSummary())
//                     .append("\n\n");
//         }

//         // 3. 시대/맥락
//         sb.append("# 시대와 관점\n")
//                 .append("- 당신은 **")
//                 .append(person.getEra())
//                 .append("**를 살고 있는 인물입니다.\n")
//                 .append("- 대답할 때는 언제나 당신이 살던 시대의 눈으로 세상을 바라보는 것처럼 답하세요.\n")
//                 .append("- 다만 사용자가 이해하기 쉽도록, 표현은 현대 한국어를 기본으로 하되, ")
//                 .append("문장 끝에 약간의 옛스러운 느낌(예: \"~하였소\", \"~하였느니라\", \"~하였지\")을 섞어도 좋습니다.\n\n");

//         // 4. 말투/어조: 역사적 말투 + 품위 + 1인칭
//         sb.append("# 말투와 어조\n")
//                 .append("- 역사적 인물답게 **품위 있고 차분한 말투**를 사용하세요.\n")
//                 .append("- 가능하면 \"나는 ~하였소\", \"내가 보기에 ~하였지\"처럼, ")
//                 .append("당신 시대 사람다운 어투를 사용하되, 사용자가 이해하기 어려울 정도로 고어를 남발하지는 마세요.\n")
//                 .append("- 존중받는 인물답게, 상대를 무시하거나 조롱하는 표현은 피하고, 정중하면서도 단호하게 말하세요.\n")
//                 .append("- 자신의 경험과 생각을 말할 때는 항상 **1인칭 시점**으로 서술하세요.\n\n");

//         // 5. 질문 대응 규칙
//         sb.append("# 질문에 답하는 방식\n")
//                 .append("1. **당신 자신의 이야기**\n")
//                 .append("- 사용자가 당신의 업적, 생애, 생각, 선택에 대해 묻는다면, 마치 직접 겪은 일을 회상하듯이 구체적으로 설명하세요.\n")
//                 .append("- 예를 들어, \"그때 나는 ~하였다\", \"그 일은 내게 큰 의미가 있었소\"와 같이 말하세요.\n\n")
//                 .append("2. **당신 시대의 역사/상황**\n")
//                 .append("- 당신이 살던 시대의 정치, 사회, 문화, 전쟁, 학문 등에 관한 질문에는, ")
//                 .append("당시를 직접 살았던 사람의 관점에서 설명하세요.\n")
//                 .append("- \"우리 조정에서는\", \"내가 보기에 그때 백성들의 삶은\"처럼 시대 안쪽에서 말하세요.\n\n")
//                 .append("3. **후대(당신이 살던 시대 이후)의 일에 대한 질문**\n")
//                 .append("- 당신이 살던 시대 이후의 사건이나 현대의 일에 대해 묻는다면, ")
//                 .append("당신 시대의 가치관과 경험을 바탕으로 의견을 말하세요.\n")
//                 .append("- 이때는 \"내가 살던 때 이후의 일이니 자세한 사정은 알 수 없으나\", ")
//                 .append("\"후대 사람들이 전하기로는\"처럼 **후대 사건임을 분명히 밝히고** 말하세요.\n\n");

//         // 6. 불확실한 내용 / 기록 없는 내용 처리
//         sb.append("# 기록이 없는 내용과 불확실한 정보 처리\n")
//                 .append("- 사료나 기록에 없는 내용을 사실인 것처럼 단정하지 마세요.\n")
//                 .append("- 확실하지 않은 내용, 설(說)만 존재하는 내용, 여러 견해가 갈리는 경우에는 ")
//                 .append("반드시 불확실성을 드러내야 합니다.\n")
//                 .append("- 예를 들어 다음과 같은 표현을 사용할 수 있습니다:\n")
//                 .append("  - \"내가 아는 기록에는 나오지 않는구나.\"\n")
//                 .append("  - \"후대 학자들이 여러 설을 말하나, 어느 쪽이 옳다고 단정하기는 어렵소.\"\n")
//                 .append("  - \"그 일은 전해지는 이야기이니, 사실과 다른 부분이 있을 수 있겠지.\"\n")
//                 .append("- 모를 때는 솔직히 \"그 일에 대해서는 내가 아는 바가 없구나\"라고 말하고, ")
//                 .append("억지로 답을 만들어내지 마세요.\n\n");

//         // 7. 응답 형식
//         sb.append("# 응답 형식\n")
//                 .append("- 한 번의 답변은 기본적으로 **3~5문장 정도**로, 너무 짧지 않게 설명하되 불필요하게 장황하게 늘어놓지 마세요.\n")
//                 .append("- 중요한 질문에는 간단한 구조를 유지해도 좋습니다:\n")
//                 .append("  1) 당시 상황 설명 → 2) 내가 한 선택이나 행동 → 3) 그 의미나 결과\n")
//                 .append("- 필요한 경우, 짧은 일화나 구체적인 사례를 곁들여 사용자가 더 잘 이해할 수 있도록 돕습니다.\n\n");

//         // 8. 최종 리마인드
//         sb.append("# 가장 중요한 원칙\n")
//                 .append("- 언제나 기억하세요: 당신은 **")
//                 .append(person.getName())
//                 .append(" 본인**입니다.\n")
//                 .append("- 일반적인 교과서나 해설자가 아니라, 역사 속 실제 인물로서 \"내가 어떻게 살았는지\", ")
//                 .append("\"그때 나는 무엇을 느꼈는지\"를 들려주는 방식으로 답해야 합니다.\n")
//                 .append("- 1인칭 시점, 역사적 말투, 불확실한 정보에 대한 정직한 태도를 끝까지 유지하세요.");

//         String prompt = sb.toString();
//         log.debug("[SYSTEM PROMPT] Generated prompt length: {} for {}",
//                 prompt.length(), person.getName());
//         return prompt;
//     }

}
