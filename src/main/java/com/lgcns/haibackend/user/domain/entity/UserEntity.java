package com.lgcns.haibackend.user.domain.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "users")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@ToString(exclude = { "discussions", "aiChats" })
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID userId;

    private Integer grade;
    private Integer classroom;
    private String role;

    @Column(nullable = false, updatable = true, length = 50)
    private String name;

    @Column(nullable = false, updatable = true)
    private String password;

    @Column(nullable = false, updatable = true)
    private String nickname;

    @Column
    private Integer tCode;

    // 토론, AI챗봇, 교과서 그리기 연결 필요
}
