package com.lgcns.haibackend.bedrock.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 채팅 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    @Builder.Default
    private String model = "claude-3-5-sonnet";

    private List<MessageDTO> messages;

    private String system;

    @JsonProperty("max_tokens")
    @Builder.Default
    private Integer maxTokens = 4096;

    @Builder.Default
    private Double temperature = 1.0;

    @JsonProperty("top_p")
    private Double topP;

    @JsonProperty("top_k")
    private Integer topK;

    @Builder.Default
    private Boolean stream = false;

    @JsonProperty("stop_sequences")
    private List<String> stopSequences;

    private Map<String, Object> metadata;
}
