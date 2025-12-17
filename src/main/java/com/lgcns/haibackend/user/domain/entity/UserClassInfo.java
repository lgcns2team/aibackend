package com.lgcns.haibackend.user.domain.entity;

import java.util.UUID;

public interface UserClassInfo {
    UUID getClassCode();
    Integer getGrade();
    Integer getClassroom();
    String getNickname();
}
