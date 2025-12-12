package com.lgcns.haibackend.bedrock.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcns.haibackend.aiPerson.domain.dto.PromptRequest;
import com.lgcns.haibackend.bedrock.domain.dto.KnowledgeBaseRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * FastAPI Bedrock Gateway 클라이언트
 * WebClient를 사용한 비동기 방식
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FastApiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${fastapi.base-url:http://localhost:8000}")
    private String baseUrl;

    /**
     * 프롬프트 템플릿 기반 채팅 요청 (스트리밍) - /chat/prompt
     */
    public Flux<String> chatPromptStream(PromptRequest request) {
        String endpoint = baseUrl + "/chat/prompt";

        log.info("🌐 [PROMPT REQUEST] promptId={}, query={}", request.getPromptId(), request.getUserQuery());

        return webClient.post()
                .uri(endpoint)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(org.springframework.core.io.buffer.DataBuffer.class)
                .transform(this::decodeAndParseSse)
                .doOnError(error -> {
                    log.error("❌ [PROMPT ERROR] {}", error.getMessage());
                })
                .doOnComplete(() -> {
                    log.info("✅ [PROMPT COMPLETE]");
                });
    }

    /**
     * Knowledge Base 검색 (스트리밍)
     */
    public Flux<String> retrieveFromKnowledgeBaseStream(KnowledgeBaseRequest request) {
        String endpoint = baseUrl + "/chat/knowledge";
        log.info("🌐 [KB REQUEST] query={}", request.getQuery());

        return webClient.post()
                .uri(endpoint)
                .bodyValue(Map.of(
                        "query", request.getQuery(),
                        "kb_id", request.getKbId(),
                        "model_arn", request.getModelArn()))
                .retrieve()
                .bodyToFlux(org.springframework.core.io.buffer.DataBuffer.class)
                .transform(this::decodeAndParseSse)
                .doOnError(error -> log.error("❌ [KB ERROR] {}", error.getMessage()));
    }

    /**
     * ✅ SSE 스트림 디코딩 및 파싱 (순서 보장)
     */
    private Flux<String> decodeAndParseSse(Flux<DataBuffer> body) {
        AtomicReference<StringBuilder> buffer = new AtomicReference<>(new StringBuilder());

        return body
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return new String(bytes, StandardCharsets.UTF_8);
                })
                // ⭐ flatMap → concatMap으로 변경 (순서 보장)
                .concatMap(text -> {
                    buffer.get().append(text);
                    String accumulated = buffer.get().toString();

                    String[] events = accumulated.split("\n\n");

                    if (events.length > 1) {
                        buffer.set(new StringBuilder(events[events.length - 1]));

                        String[] completeEvents = new String[events.length - 1];
                        System.arraycopy(events, 0, completeEvents, 0, events.length - 1);

                        return Flux.fromArray(completeEvents);
                    }

                    return Flux.empty();
                })
                // ⭐ 스트림 종료 시 남은 버퍼 처리
                .concatWith(Flux.defer(() -> {
                    String remaining = buffer.get().toString().trim();
                    if (!remaining.isEmpty()) {
                        log.info("🔚 [FINAL BUFFER] {}", remaining);
                        return Flux.just(remaining);
                    }
                    return Flux.empty();
                }))
                // ⭐ 여기도 concatMap으로 변경
                .concatMap(event -> {
                    String[] lines = event.split("\n");
                    return Flux.fromArray(lines)
                            // ⭐ 여기도 concatMap으로 변경
                            .concatMap(line -> {
                                if (line.contains("data: ")) {
                                    String[] parts = line.split("(?=data: )");
                                    return Flux.fromArray(parts);
                                }
                                return Flux.just(line);
                            })
                            .filter(line -> line.startsWith("data: "))
                            .map(line -> line.substring(6).trim());
                })
                .filter(data -> !data.isEmpty() && !data.equals("[DONE]"))
                .map(data -> {
                    try {
                        Map<String, Object> chunk = objectMapper.readValue(data, Map.class);
                        String type = (String) chunk.get("type");

                        if ("content".equals(type)) {
                            return (String) chunk.getOrDefault("text", "");
                        } else if ("done".equals(type)) {
                            log.info("✅ [STREAM DONE]");
                            return "";
                        }
                        return "";
                    } catch (Exception e) {
                        log.error("❌ [JSON PARSE ERROR] data={}", data, e);
                        return "";
                    }
                })
                .filter(text -> !text.isEmpty());
    }

    /**
     * 헬스 체크
     */
    public boolean isHealthy() {
        try {
            Map<String, Object> health = webClient.get()
                    .uri(baseUrl + "/health")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return health != null && "healthy".equals(health.get("status"));
        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage());
            return false;
        }
    }
}