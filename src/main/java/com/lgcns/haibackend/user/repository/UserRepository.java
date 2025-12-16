package com.lgcns.haibackend.user.repository;


import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lgcns.haibackend.user.domain.entity.UserEntity;


@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    public Optional<UserEntity> findByNameAndPassword(String name, String password);
    public Optional<UserEntity> findByUserId(UUID userId);
}
