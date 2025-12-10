package com.lgcns.haibackend.aiPerson.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lgcns.haibackend.aiPerson.domain.dto.AIPersonDetailDTO;
import com.lgcns.haibackend.aiPerson.domain.dto.AIPersonListDTO;
import com.lgcns.haibackend.aiPerson.service.AIPersonService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai-person")
@RequiredArgsConstructor
public class AIPersonController {
    
    private final AIPersonService aiPersonService;

    @GetMapping
    public ResponseEntity<List<AIPersonListDTO>> getAllPersons() {
        List<AIPersonListDTO> personList = aiPersonService.getAllPersons();
        return ResponseEntity.ok(personList);
    }

    @GetMapping("/{promptId}")
    public ResponseEntity<AIPersonDetailDTO> getPersonDetail(@PathVariable("promptId") String promptId) {
        AIPersonDetailDTO detail = aiPersonService.getPersonDetail(promptId);
        return ResponseEntity.ok(detail);
    }
}
