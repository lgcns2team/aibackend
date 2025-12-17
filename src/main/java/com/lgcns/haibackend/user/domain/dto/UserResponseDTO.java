package com.lgcns.haibackend.user.domain.dto;

import java.util.UUID;

import com.lgcns.haibackend.common.Role;
import com.lgcns.haibackend.user.domain.entity.UserEntity;

import io.swagger.v3.oas.annotations.media.Schema;
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
    private Role role;

    // 선생님 가입 시에만 반환되는, 생성된 반 코드
    @Schema(description = "선생님 가입 시 생성된 6자리 반 초대 코드", example = "123456")
    private String createdTeacherCode;

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
