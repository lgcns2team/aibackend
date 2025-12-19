package com.lgcns.haibackend.aiPerson.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class PromptRequest {
    @JsonProperty("prompt_id")
    private String promptId;

    @JsonProperty("user_query")
    private String userQuery;

    @Builder.Default
    private Map<String, String> variables = new HashMap<>();

}
