package com.lgcns.haibackend.discussion.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessage {
    private MessageType type;
    private String content;
    private String sender;
    private String roomId;
    private UUID userId;

    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE
    }
}
