package com.lgcns.haibackend.discussion.domain.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class DebateRoomResponseDTO {
    private UUID roomId;
    private UUID teacherId;
    private UUID classCode;
    private Integer participantCount;
    private String topicTitle;
    private String topicDescription;
    private LocalDateTime createdAt;
}
