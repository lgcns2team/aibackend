package com.lgcns.haibackend.bedrock.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 채팅 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String id;
    private String model;
    private String content;
    private Map<String, Integer> usage;

    @JsonProperty("stop_reason")
    private String stopReason;

    private String timestamp;
}
