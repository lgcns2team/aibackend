package com.lgcns.haibackend.user.domain.dto;

import java.util.UUID;

import com.lgcns.haibackend.user.domain.entity.UserEntity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserRequestDTO {
    private String name;
    private String password;
    private String nickname;
    private Integer grade;
    private Integer classroom;
    private String role;

    public UserEntity toEntity() {
        return UserEntity.builder()
                .name(this.name)
                .password(this.password)
                .nickname(this.nickname)
                .grade(this.grade)
                .classroom(this.classroom)
                .role(this.role)
                .build();
    }
}
