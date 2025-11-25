package com.khi.voiceservice.repository;

import com.khi.voiceservice.Entity.Transcript;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TranscriptRepository extends JpaRepository<Transcript, Long> {
    Page<Transcript> findByUserId(String userId, Pageable pageable);
}
