package com.khi.chatservice.domain.repository;

import com.khi.chatservice.domain.entity.ChatRoomParticipantEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomParticipantRepository
        extends JpaRepository<ChatRoomParticipantEntity, Long> {

    List<ChatRoomParticipantEntity> findByRoomId(Long roomId);

    Page<ChatRoomParticipantEntity> findByUserId(String userId, Pageable pageable);

    Optional<ChatRoomParticipantEntity> findByRoomIdAndUserId(Long roomId, String userId);
}
