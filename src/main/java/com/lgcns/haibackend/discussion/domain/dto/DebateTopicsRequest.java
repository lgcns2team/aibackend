package com.lgcns.haibackend.discussion.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 토론 주제 추천 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebateTopicsRequest {
    @JsonProperty("user_query")
    private String userQuery;
}
