package com.lgcns.haibackend.bedrock.domain.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Knowledge Base 검색 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseRequest {
    private String query;
    private String kbId;
    private String modelArn;
    
    @Builder.Default
    private Integer maxResults = 5;

    // 대화 기록을 담을 필드
    // private List<Message> history;
}
