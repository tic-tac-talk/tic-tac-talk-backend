package com.khi.chatservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "chat_room_read_status", uniqueConstraints = @UniqueConstraint(columnNames = {"chat_room_id", "user_id"}))
public class ChatRoomReadStatusEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoomEntity chatRoom;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private Long lastReadMessageId;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void updateLastRead(Long messageId) {
        this.lastReadMessageId = messageId;
        this.updatedAt = LocalDateTime.now();
    }
} 