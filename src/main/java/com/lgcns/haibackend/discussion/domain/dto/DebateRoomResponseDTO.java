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
    private Integer tCode;
    private Integer participantCount;
    private String topicTitle;
    private String topicDescription;
    private LocalDateTime createdAt;

    public static DebateRoomResponseDTO from(Map<Object, Object> map) {
    return DebateRoomResponseDTO.builder()
            .roomId(UUID.fromString((String) map.get("roomId")))
            .teacherId(UUID.fromString((String) map.get("teacherId")))
            .tCode(Integer.parseInt((String) map.get("tCode")))
            .participantCount(Integer.parseInt((String) map.getOrDefault("participantCount", "0")))
            .topicTitle((String) map.get("topicTitle"))
            .topicDescription((String) map.get("topicDescription"))
            .createdAt(LocalDateTime.parse((String) map.get("createdAt")))
            .build();
}

}
