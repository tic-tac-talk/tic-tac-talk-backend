package com.khi.chatservice.domain.repository;

import com.khi.chatservice.domain.entity.ChatMessageEntity;
import com.khi.chatservice.domain.entity.ChatRoomEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessageEntity, Long> {

    Slice<ChatMessageEntity> findByRoomIdOrderBySentAtDesc(Long roomId, Pageable pageable);

    List<ChatMessageEntity> findByRoomIdOrderBySentAtAsc(Long roomId);

    ChatMessageEntity findTopByRoomOrderBySentAtDesc(ChatRoomEntity room);

    int countByRoomIdAndIdGreaterThanAndSenderIdNot(Long id, Long lastReadMessageId, String userId);
}
