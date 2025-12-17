package com.lgcns.haibackend.user.repository;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lgcns.haibackend.user.domain.entity.UserEntity;


@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    public Optional<UserEntity> findByNameAndPassword(String name, String password);
    public Optional<UserEntity> findByUserId(UUID userId);

    @Query("select u.classCode from UserEntity u where u.userId = :userId")
    UUID findClassCodeByUserId(@Param("userId") UUID userId);

    @Query("select u.nickname from UserEntity u where u.userId = :userId")
    String findNickNameByUserId(@Param("userId") UUID userId);
}
