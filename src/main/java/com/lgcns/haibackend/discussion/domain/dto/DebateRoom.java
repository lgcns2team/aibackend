package com.lgcns.haibackend.discussion.domain.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DebateRoom {

    private UUID roomId;
    private UUID topicId;
    private UUID teacherId;
    private boolean isActive = true; // 기본값은 활성(true)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer participantsCount = 0; // 기본값 0
    private String topicTitle;
    private String topicDescription;

}
