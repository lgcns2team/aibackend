package com.lgcns.haibackend.bedrock.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 메시지 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private String role;
    private String content;

    public static MessageDTO user(String content) {
        return MessageDTO.builder()
                .role("user")
                .content(content)
                .build();
    }

    public static MessageDTO assistant(String content) {
        return MessageDTO.builder()
                .role("assistant")
                .content(content)
                .build();
    }

    public static MessageDTO system(String content) {
        return MessageDTO.builder()
                .role("system")
                .content(content)
                .build();
    }
}
