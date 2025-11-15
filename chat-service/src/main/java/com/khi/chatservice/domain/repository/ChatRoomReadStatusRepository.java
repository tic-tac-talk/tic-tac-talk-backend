package com.khi.chatservice.domain.repository;

import com.khi.chatservice.domain.entity.ChatRoomEntity;
import com.khi.chatservice.domain.entity.ChatRoomReadStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomReadStatusRepository extends JpaRepository<ChatRoomReadStatusEntity, Long> {
    Optional<ChatRoomReadStatusEntity> findByChatRoomAndUserId(ChatRoomEntity chatRoom, String userId);
} 