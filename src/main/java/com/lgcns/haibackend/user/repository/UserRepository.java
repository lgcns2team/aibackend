package com.lgcns.haibackend.user.repository;

import com.lgcns.haibackend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}
