package com.lgcns.haibackend.discussion.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessage {
    private String id;
    private String parentId;
    private java.util.UUID userId;
    private MessageType type;
    private String content;
    private String sender;
    private DebateStatus status;
    private LocalDateTime createdAt;

    public enum MessageType {
        CHAT,
        STATUS,
        JOIN,
        LEAVE, MODE_CHANGE
    }
}
