package com.lgcns.haibackend.discussion.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 토론 주제 추천 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebateTopicsResponse {

    @JsonProperty("debate_topics")
    private List<DebateTopicDTO> debateTopics;

}
