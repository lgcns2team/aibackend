package com.lgcns.haibackend.bedrock.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 모델 목록 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelsResponse {
    private List<Model> models;
}
