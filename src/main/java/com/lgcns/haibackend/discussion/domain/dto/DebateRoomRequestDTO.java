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
public class DebateRoomRequestDTO {

    private Integer participantCount;
    private String topicTitle;
    private String topicDescription;
    private Integer grade;
    private Integer classroom;
}
