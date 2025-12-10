package com.lgcns.haibackend.bedrock.client;

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
public class Message {
    private String role;
    private String content;

    public static Message user(String content) {
        return Message.builder()
                .role("user")
                .content(content)
                .build();
    }

    public static Message assistant(String content) {
        return Message.builder()
                .role("assistant")
                .content(content)
                .build();
    }

    public static Message system(String content) {
        return Message.builder()
                .role("system")
                .content(content)
                .build();
    }
}
