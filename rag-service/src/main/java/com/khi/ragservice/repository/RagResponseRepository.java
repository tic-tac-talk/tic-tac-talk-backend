package com.khi.ragservice.repository;

import com.khi.ragservice.entity.RagResponseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RagResponseRepository extends JpaRepository<RagResponseEntity, Long> {
    List<RagResponseEntity> findByUserId(String userId);
}
