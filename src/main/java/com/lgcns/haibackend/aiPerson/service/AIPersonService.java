package com.lgcns.haibackend.aiPerson.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lgcns.haibackend.aiPerson.domain.dto.AIPersonDetailDTO;
import com.lgcns.haibackend.aiPerson.domain.dto.AIPersonListDTO;
import com.lgcns.haibackend.aiPerson.domain.entity.AIPersonEntity;
import com.lgcns.haibackend.aiPerson.repository.AIPersonRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AIPersonService {
    
    private final AIPersonRepository aiPersonRepository;

    public List<AIPersonListDTO> getAllPersons() {
        return aiPersonRepository.findAll()
                .stream()
                .map(AIPersonListDTO::fromEntity)
                .sorted(Comparator.comparing(AIPersonListDTO::getYear))
                .toList();
    }

    public AIPersonDetailDTO getPersonDetail(String promptId) {
        AIPersonEntity entity = aiPersonRepository.findById(promptId)
                .orElseThrow(() -> new IllegalArgumentException("AI 인물을 찾을 수 없습니다. promptId=" + promptId));

        return AIPersonDetailDTO.fromEntity(entity);
    }

}
