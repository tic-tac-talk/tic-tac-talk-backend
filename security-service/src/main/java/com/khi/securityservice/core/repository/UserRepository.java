package com.khi.securityservice.core.repository;

import com.khi.securityservice.core.entity.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findOptionalByUid(String uid);

    UserEntity findByUid(String uid);
}
