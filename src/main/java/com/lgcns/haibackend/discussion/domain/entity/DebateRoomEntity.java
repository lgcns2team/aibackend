package com.lgcns.haibackend.discussion.domain.entity;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "debateRoom")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DebateRoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID roomId;

    @Column(nullable = false) // NOT NULL 제약 조건
    private Integer topicId;

    @Column(nullable = false)
    private boolean isActive = true; // 기본값은 활성(true)

    @CreationTimestamp
    @Column(nullable = false, updatable = false) // 생성 후 수정 불가능 (updatable=false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @Column(nullable = false)
    private Integer participantsCount = 0; // 기본값 0

}
