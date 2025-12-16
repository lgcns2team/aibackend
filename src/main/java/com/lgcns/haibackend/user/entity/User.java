package com.lgcns.haibackend.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    private Integer grade;

    @Column(length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String nickname;

    private String password; // In real app, this should be encrypted

    @Column(name = "class")
    private Integer classNumber;

    public enum Role {
        TEACHER,
        STUDENT
    }
}
