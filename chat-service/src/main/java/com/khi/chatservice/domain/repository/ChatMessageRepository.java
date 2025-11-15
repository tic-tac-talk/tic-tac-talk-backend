package com.khi.chatservice.domain.repository;

import com.khi.chatservice.domain.entity.ChatMessageEntity;
import com.khi.chatservice.domain.entity.ChatRoomEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessageEntity, Long> {

    Slice<ChatMessageEntity> findByRoomIdOrderBySentAtDesc(Long roomId, Pageable pageable);

    ChatMessageEntity findTopByRoomOrderBySentAtDesc(ChatRoomEntity room);

    int countByRoomIdAndIdGreaterThanAndSenderIdNot(Long id, Long lastReadMessageId, String userId);
}
