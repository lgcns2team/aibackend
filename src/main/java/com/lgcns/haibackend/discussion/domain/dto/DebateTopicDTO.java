package com.lgcns.haibackend.discussion.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 단일 토론 주제 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebateTopicDTO {

    @JsonProperty("topic")
    private String topic;

    @JsonProperty("description")
    private String description;
}
