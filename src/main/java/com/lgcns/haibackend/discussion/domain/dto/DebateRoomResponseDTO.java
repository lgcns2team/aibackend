package com.lgcns.haibackend.discussion.domain.dto;

import java.time.LocalDateTime;
import java.util.Map;
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
    private Integer teacherCode;
    private Integer participantCount;
    private String topicTitle;
    private String topicDescription;
    private LocalDateTime createdAt;
    private String viewMode;
    private Integer grade;
    private Integer classroom;

    public static DebateRoomResponseDTO from(Map<Object, Object> map) {
        return DebateRoomResponseDTO.builder()
                .roomId(UUID.fromString((String) map.get("roomId")))
                .teacherId(UUID.fromString((String) map.get("teacherId")))
                .teacherCode(Integer.parseInt((String) map.get("teacherCode")))
                .participantCount(Integer.parseInt((String) map.getOrDefault("participantCount", "0")))
                .topicTitle((String) map.get("topicTitle"))
                .topicDescription((String) map.get("topicDescription"))
                .viewMode((String) map.getOrDefault("viewMode", "vote"))
                .createdAt(LocalDateTime.parse((String) map.get("createdAt")))
                .grade(map.get("grade") != null ? Integer.parseInt((String) map.get("grade")) : null)
                .classroom(map.get("classroom") != null ? Integer.parseInt((String) map.get("classroom")) : null)
                .build();
    }

}
