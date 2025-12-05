package com.lgcns.haibackend.bedrock.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

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
     * 일반 채팅 완성 요청 (동기식)
     */
    public ChatResponse chat(ChatRequest request) {
        return webClient.post()
                .uri(baseUrl + "/chat")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .doOnError(error -> log.error("Error calling FastAPI: {}", error.getMessage()))
                .block();
    }

    /**
     * 일반 채팅 완성 요청 (비동기)
     */
    public Mono<ChatResponse> chatAsync(ChatRequest request) {
        return webClient.post()
                .uri(baseUrl + "/chat")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .doOnSuccess(response -> log.info("Received response from FastAPI"))
                .doOnError(error -> log.error("Error calling FastAPI: {}", error.getMessage()));
    }

    /**
     * 스트리밍 채팅 요청
     */
    public Flux<String> chatStream(ChatRequest request) {
        request.setStream(true);

        return webClient.post()
                .uri(baseUrl + "/chat")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(org.springframework.core.io.buffer.DataBuffer.class)
                .transform(this::decodeAndParseSse)
                .doOnError(error -> log.error("Streaming error: {}", error.getMessage()));
    }

    /**
     * 간단한 메시지 전송
     */
    public String sendSimpleMessage(String message) {
        SimpleChatRequest request = SimpleChatRequest.builder()
                .message(message)
                .model("claude-3-5-sonnet")
                .stream(false)
                .build();

        ChatResponse response = webClient.post()
                .uri(baseUrl + "/chat/simple")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .block();

        return response != null ? response.getContent() : null;
    }

    /**
     * 사용 가능한 모델 목록 조회
     */
    public List<Model> getModels() {
        ModelsResponse response = webClient.get()
                .uri(baseUrl + "/models")
                .retrieve()
                .bodyToMono(ModelsResponse.class)
                .block();

        return response != null ? response.getModels() : List.of();
    }

    /**
     * Knowledge Base 검색 (스트리밍)
     */
    public Flux<String> retrieveFromKnowledgeBaseStream(KnowledgeBaseRequest request) {
        return webClient.post()
                .uri(baseUrl + "/chat/knowledge")
                .bodyValue(Map.of(
                        "query", request.getQuery(),
                        "kb_id", request.getKbId(),
                        "model_arn", request.getModelArn()))
                .retrieve()
                .bodyToFlux(org.springframework.core.io.buffer.DataBuffer.class)
                .transform(this::decodeAndParseSse)
                .doOnError(error -> log.error("Knowledge Base streaming error: {}", error.getMessage()));
    }

    /**
     * SSE 스트림 디코딩 및 파싱 헬퍼
     * DataBuffer -> String (UTF-8 safe) -> Lines -> SSE Data
     */
    private Flux<String> decodeAndParseSse(Flux<org.springframework.core.io.buffer.DataBuffer> body) {
        return body
                // UTF-8 안전하게 디코딩 (문자 경계를 존중)
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    org.springframework.core.io.buffer.DataBufferUtils.release(dataBuffer);
                    return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                })
                // 라인별로 분리
                .flatMap(text -> {
                    String[] lines = text.split("\n");
                    return Flux.fromArray(lines);
                })
                // SSE 데이터 라인만 필터링
                .filter(line -> line.startsWith("data: "))
                .map(line -> line.substring(6).trim())
                .filter(data -> !data.isEmpty() && !data.equals("[DONE]"))
                // JSON 파싱
                .map(data -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> chunk = objectMapper.readValue(data, Map.class);
                        String type = (String) chunk.get("type");

                        if ("content".equals(type)) {
                            String text = (String) chunk.getOrDefault("text", "");
                            log.debug("📦 [CHUNK] Received content: {}",
                                    text.substring(0, Math.min(30, text.length())));
                            return text;
                        } else if ("citations".equals(type)) {
                            // Citations 정보 로그
                            log.info("📚 [CITATIONS] Received {} citations", chunk.get("count"));
                            return "";
                        } else if ("done".equals(type)) {
                            log.info("✅ [STREAM DONE] Total length: {}", chunk.get("total_length"));
                            return "";
                        } else if ("error".equals(type)) {
                            log.error("❌ [ERROR] {}", chunk.get("message"));
                            return "";
                        }

                        // 일반 채팅 응답 (type 필드가 없을 수 있음)
                        return (String) chunk.getOrDefault("content", "");
                    } catch (Exception e) {
                        log.error("Error parsing JSON chunk: {} - Data: {}", e.getMessage(), data);
                        // JSON 파싱 실패 시 원본 데이터 그대로 반환 (fallback)
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