package com.lgcns.haibackend.user.domain.dto;

import java.util.UUID;

import com.lgcns.haibackend.user.domain.entity.UserEntity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponseDTO {
    private UUID userId; // DB에서 생성된 UUID 타입
    
    private Integer grade;
    private Integer classroom;
    private String name;
    private String nickname;
    private String role;

    public static UserResponseDTO fromEntity(UserEntity user) {
        return UserResponseDTO.builder()
                .userId(user.getUserId())
                .grade(user.getGrade())
                .classroom(user.getClassroom())
                .name(user.getName())
                .nickname(user.getNickname())
                .role(user.getRole())
                .build();
    }
}
