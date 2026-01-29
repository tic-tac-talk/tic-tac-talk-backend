package com.khi.chatservice.util;

import com.khi.chatservice.application.ChatService;
import com.khi.chatservice.client.UserClient;
import com.khi.chatservice.client.dto.UserInfo;
import com.khi.chatservice.domain.entity.ChatMessageEntity;
import com.khi.chatservice.domain.entity.ChatRoomParticipantEntity;
import com.khi.chatservice.domain.entity.SocketEventType;
import com.khi.chatservice.domain.repository.ChatRoomParticipantRepository;
import com.khi.chatservice.presentation.dto.res.ChatRoomListRes;
import com.khi.chatservice.presentation.dto.res.ChatSocketRes;
import com.khi.chatservice.presentation.dto.res.UserJoinedRes;
import com.khi.chatservice.redis.RedisPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventBroadcaster {
    private final RedisPublisher redisPublisher;
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

        redisPublisher.publish(
                SocketEventType.NEW_MESSAGE,
                "/topic/room/" + roomUuid,
                dto
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
                redisPublisher.publish(
                        SocketEventType.CHAT_ROOM_UPDATE,
                        topicDestination,
                        summary
                );

                log.info("CHAT_ROOM_UPDATE sent to user: {}", uid);

            } catch (Exception e) {
                log.error("Failed to send CHAT_ROOM_UPDATE to user {}: {}", uid, e.getMessage());
            }
        });
    }

    public void broadcastMessageRead(String roomUuid, Long lastReadMessageId) {
        redisPublisher.publish(
                SocketEventType.MESSAGE_READ,
                "/topic/room/" + roomUuid,
                lastReadMessageId
        );
        log.info("MESSAGE_READ → /topic/room/{}", roomUuid);
    }

    public void sendMessageToUser(String userId, Object message) {
        String topicDestination = "/topic/user-messages/" + userId;
        try {
            redisPublisher.publish(
                    SocketEventType.SEND_MESSAGE,
                    topicDestination,
                    message
            );
            log.info("sendMessageToUser success - userId: {}", userId);
        } catch (Exception e) {
            log.error("sendMessageToUser failed - userId: {}, error: {}", userId, e.getMessage());
        }
    }

    public void broadcastChatEndToAll(String roomUuid, String reportId) {
        String topicDestination = "/topic/room/" + roomUuid;
        try {
            HashMap<String, String> payload = new HashMap<>();
            payload.put("message", "채팅이 종료되었습니다.");
            payload.put("reportId", reportId);

            redisPublisher.publish(
                    SocketEventType.CHAT_END,
                    topicDestination,
                    payload
            );
            log.info("CHAT_END sent to all users in room: {}, reportId: {}", roomUuid, reportId);
        } catch (Exception e) {
            log.error("Failed to send CHAT_END to room {}: {}", roomUuid, e.getMessage());
        }
    }

    public void broadcastUserJoined(String roomUuid, String userId) {
        UserInfo joinedUser = userClient.getUserInfo(userId);
        String userNickname = joinedUser != null ? joinedUser.nickname() : "사용자";

        List<String> participantIds = partRepo.findByRoom_RoomUuid(roomUuid).stream()
                .map(ChatRoomParticipantEntity::getUserId)
                .collect(Collectors.toList());

        List<UserJoinedRes.ParticipantInfo> participants = userClient.getUserInfos(participantIds).stream()
                .map(userInfo -> UserJoinedRes.ParticipantInfo.builder()
                        .userId(userInfo.userId())
                        .nickname(userInfo.nickname())
                        .profileUrl(userInfo.profileUrl())
                        .build())
                .collect(Collectors.toList());

        UserJoinedRes response = UserJoinedRes.builder()
                .participants(participants)
                .build();

        String topicDestination = "/topic/room/" + roomUuid;
        try {
            redisPublisher.publish(
                    SocketEventType.USER_JOINED,
                    topicDestination,
                    response
            );
            log.info("USER_JOINED sent to room: {}, userId: {}, nickname: {}, participantCount: {}",
                    roomUuid, userId, userNickname, participants.size());
        } catch (Exception e) {
            log.error("Failed to send USER_JOINED to room {}: {}", roomUuid, e.getMessage());
        }
    }
}
