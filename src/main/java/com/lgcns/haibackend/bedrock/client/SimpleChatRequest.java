package com.lgcns.haibackend.bedrock.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 간단한 채팅 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleChatRequest {
    private String message;

    @Builder.Default
    private String model = "claude-3-5-sonnet";

    @Builder.Default
    private Boolean stream = false;
}
