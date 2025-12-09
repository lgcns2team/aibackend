package com.lgcns.haibackend.aiPerson.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lgcns.haibackend.aiPerson.domain.dto.AIPersonListDTO;
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
                .toList();
    }

}
