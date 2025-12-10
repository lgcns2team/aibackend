package com.lgcns.haibackend.aiPerson.domain.dto;

import com.lgcns.haibackend.aiPerson.domain.entity.AIPersonEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AIPersonDetailDTO {
    
    private String name;
    private String summary;
    private String exQuestion;
    private String greetingMessage;
    private String era;

    public static AIPersonDetailDTO fromEntity(AIPersonEntity aiPerson) {
        return AIPersonDetailDTO.builder()
                .name(aiPerson.getName())
                .summary(aiPerson.getSummary())
                .exQuestion(aiPerson.getExQuestion())
                .greetingMessage(aiPerson.getGreetingMessage())
                .build();
    }
}
