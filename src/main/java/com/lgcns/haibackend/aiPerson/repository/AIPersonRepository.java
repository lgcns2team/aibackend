package com.lgcns.haibackend.aiPerson.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lgcns.haibackend.aiPerson.domain.entity.AIPersonEntity;

@Repository
public interface AIPersonRepository extends JpaRepository<AIPersonEntity, String> {
    
}
