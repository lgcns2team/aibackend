package com.lgcns.haibackend.bedrock.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 모델 정보 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Model {
    private String id;

    @JsonProperty("full_id")
    private String fullId;

    private String name;
    private String provider;

    @JsonProperty("context_window")
    private Integer contextWindow;

    @JsonProperty("max_output")
    private Integer maxOutput;
}
