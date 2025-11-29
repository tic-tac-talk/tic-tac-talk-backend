package com.khi.chatservice.util;

import com.khi.chatservice.application.ChatService;
import com.khi.chatservice.client.UserClient;
import com.khi.chatservice.client.dto.UserInfo;
import com.khi.chatservice.domain.entity.ChatMessageEntity;
import com.khi.chatservice.domain.entity.SocketEventType;
import com.khi.chatservice.domain.repository.ChatRoomParticipantRepository;
import com.khi.chatservice.presentation.dto.SocketEvent;
import com.khi.chatservice.presentation.dto.res.ChatRoomListRes;
import com.khi.chatservice.presentation.dto.res.ChatSocketRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventBroadcaster {
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomParticipantRepository partRepo;
    private final ChatService chatService;
    private final UserClient userClient;

    public void broadcastNewMessage(ChatMessageEntity savedMsg, String senderId) {
        Long roomId = savedMsg.getRoom().getId();
        String roomUuid = savedMsg.getRoom().getRoomUuid();

        UserInfo sender = userClient.getUserInfo(senderId);
        String senderNickname = sender != null ? sender.nickname() : null;

        ChatSocketRes dto = new ChatSocketRes(
                savedMsg.getId(),
                savedMsg.getSenderId(),
                senderNickname,
                savedMsg.getContent(),
                savedMsg.getSentAt()
        );

        messagingTemplate.convertAndSend(
                "/topic/room/" + roomUuid,
                new SocketEvent<>(SocketEventType.NEW_MESSAGE, dto)
        );
        broadcastChatRoomUpdate(roomId);
        log.info("broadcast → /topic/room/{}", roomUuid);
    }

    public void broadcastChatRoomUpdate(Long roomId) {
        var participants = partRepo.findByRoomId(roomId);

        participants.forEach(part -> {
            String uid = String.valueOf(part.getUserId());
            ChatRoomListRes.ChatRoomSummary summary = chatService.getChatRoomSummary(roomId, uid);

            String topicDestination = "/topic/user-room-updates/" + uid;

            try {
                messagingTemplate.convertAndSend(
                        topicDestination,
                        new SocketEvent<>(SocketEventType.CHAT_ROOM_UPDATE, summary));

                log.info("CHAT_ROOM_UPDATE sent to user: {}", uid);

            } catch (Exception e) {
                log.error("Failed to send CHAT_ROOM_UPDATE to user {}: {}", uid, e.getMessage());
            }
        });
    }

    public void broadcastMessageRead(String roomUuid, Long lastReadMessageId) {
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomUuid,
                new SocketEvent<>(SocketEventType.MESSAGE_READ, lastReadMessageId)
        );
        log.info("MESSAGE_READ → /topic/room/{}", roomUuid);
    }

    public void sendMessageToUser(String userId, Object message) {
        String topicDestination = "/topic/user-messages/" + userId;
        try {
            messagingTemplate.convertAndSend(
                    topicDestination,
                    new SocketEvent<>(SocketEventType.SEND_MESSAGE, message)
            );
            log.info("sendMessageToUser success - userId: {}", userId);
        } catch (Exception e) {
            log.error("sendMessageToUser failed - userId: {}, error: {}", userId, e.getMessage());
        }
    }


    public void broadcastChatEndToAll(String roomUuid, String reportId) {
        // 양쪽 사용자 모두에게 CHAT_END 알림 전송
        String topicDestination = "/topic/room/" + roomUuid;
        try {
            messagingTemplate.convertAndSend(
                    topicDestination,
                    new SocketEvent<>(SocketEventType.CHAT_END,
                            new java.util.HashMap<String, String>() {{
                                put("message", "채팅이 종료되었습니다.");
                                put("reportId", reportId);
                            }})
            );
            log.info("CHAT_END sent to all users in room: {}, reportId: {}", roomUuid, reportId);
        } catch (Exception e) {
            log.error("Failed to send CHAT_END to room {}: {}", roomUuid, e.getMessage());
        }
    }

    public void broadcastUserJoined(String roomUuid, String userId) {
        // 채팅방의 모든 사용자에게 새 사용자 참여 알림
        UserInfo joinedUser = userClient.getUserInfo(userId);
        String userNickname = joinedUser != null ? joinedUser.nickname() : "사용자";

        String topicDestination = "/topic/room/" + roomUuid;
        try {
            messagingTemplate.convertAndSend(
                    topicDestination,
                    new SocketEvent<>(SocketEventType.USER_JOINED,
                            new java.util.HashMap<String, String>() {{
                                put("userId", userId);
                                put("nickname", userNickname);
                                put("message", userNickname + "님이 채팅방에 참여했습니다.");
                            }}));
            log.info("USER_JOINED sent to room: {}, userId: {}, nickname: {}", roomUuid, userId, userNickname);
        } catch (Exception e) {
            log.error("Failed to send USER_JOINED to room {}: {}", roomUuid, e.getMessage());
        }
    }
}