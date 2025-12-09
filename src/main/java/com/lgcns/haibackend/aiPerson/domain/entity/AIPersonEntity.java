package com.lgcns.haibackend.aiPerson.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ai_person")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIPersonEntity {

    @Id
    @Column(nullable = false, unique = true)
    private String promptId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String era;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String exQuestion;

    @Column(columnDefinition = "TEXT")
    private String greetingMessage;

    @Column
    private Integer year;
}
