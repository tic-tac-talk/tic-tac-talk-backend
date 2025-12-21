package com.khi.chatservice.util;

import com.khi.chatservice.application.ChatService;
import com.khi.chatservice.client.UserClient;
import com.khi.chatservice.client.dto.UserInfo;
import com.khi.chatservice.domain.entity.ChatMessageEntity;
import com.khi.chatservice.domain.entity.ChatRoomParticipantEntity;
import com.khi.chatservice.domain.entity.SocketEventType;
import com.khi.chatservice.domain.repository.ChatRoomParticipantRepository;
import com.khi.chatservice.presentation.dto.SocketEvent;
import com.khi.chatservice.presentation.dto.res.ChatRoomListRes;
import com.khi.chatservice.presentation.dto.res.ChatSocketRes;
import com.khi.chatservice.presentation.dto.res.UserJoinedRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

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
        // 참여한 사용자 정보 조회
        UserInfo joinedUser = userClient.getUserInfo(userId);
        String userNickname = joinedUser != null ? joinedUser.nickname() : "사용자";

        // 채팅방의 모든 참여자 정보 조회
        List<String> participantIds = partRepo.findByRoom_RoomUuid(roomUuid).stream()
                .map(ChatRoomParticipantEntity::getUserId)
                .collect(Collectors.toList());

        // 모든 참여자의 상세 정보 조회
        List<UserJoinedRes.ParticipantInfo> participants = userClient.getUserInfos(participantIds).stream()
                .map(userInfo -> UserJoinedRes.ParticipantInfo.builder()
                        .userId(userInfo.userId())
                        .nickname(userInfo.nickname())
                        .profileUrl(userInfo.profileUrl())
                        .build())
                .collect(Collectors.toList());

        // USER_JOINED 응답 생성
        UserJoinedRes response = UserJoinedRes.builder()
                .participants(participants)
                .build();

        // 채팅방의 모든 사용자에게 브로드캐스트
        String topicDestination = "/topic/room/" + roomUuid;
        try {
            messagingTemplate.convertAndSend(
                    topicDestination,
                    new SocketEvent<>(SocketEventType.USER_JOINED, response)
            );
            log.info("USER_JOINED sent to room: {}, userId: {}, nickname: {}, participantCount: {}",
                    roomUuid, userId, userNickname, participants.size());
        } catch (Exception e) {
            log.error("Failed to send USER_JOINED to room {}: {}", roomUuid, e.getMessage());
        }
    }

    public void sendTokenExpiredToUser(String userId) {
        String topicDestination = "/topic/user-room-updates/" + userId;
        try {
            messagingTemplate.convertAndSend(
                    topicDestination,
                    new SocketEvent<>(SocketEventType.TOKEN_EXPIRED,
                            new java.util.HashMap<String, String>() {{
                                put("message", "액세스 토큰이 만료되었습니다.");
                            }})
            );
            log.info("TOKEN_EXPIRED sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send TOKEN_EXPIRED to user {}: {}", userId, e.getMessage());
        }
    }
}