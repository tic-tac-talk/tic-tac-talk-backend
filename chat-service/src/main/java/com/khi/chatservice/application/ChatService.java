package com.khi.chatservice.application;

import com.khi.chatservice.client.RagClient;
import com.khi.chatservice.client.UserClient;
import com.khi.chatservice.client.dto.ChatMessageDto;
import com.khi.chatservice.client.dto.RagRequestDto;
import com.khi.chatservice.client.dto.UserInfo;
import com.khi.chatservice.common.exception.type.ApiException;
import com.khi.chatservice.domain.entity.ChatMessageEntity;
import com.khi.chatservice.domain.entity.ChatRoomEntity;
import com.khi.chatservice.domain.entity.ChatRoomParticipantEntity;
import com.khi.chatservice.domain.entity.ChatRoomReadStatusEntity;
import com.khi.chatservice.domain.repository.ChatMessageRepository;
import com.khi.chatservice.domain.repository.ChatRoomParticipantRepository;
import com.khi.chatservice.domain.repository.ChatRoomReadStatusRepository;
import com.khi.chatservice.domain.repository.ChatRoomRepository;
import com.khi.chatservice.presentation.dto.res.ChatHistoryRes;
import com.khi.chatservice.presentation.dto.res.ChatMessageRes;
import com.khi.chatservice.presentation.dto.res.ChatRoomListRes;
import com.khi.chatservice.presentation.dto.res.CreateRoomRes;
import com.khi.chatservice.presentation.dto.res.SliceInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository roomRepo;
    private final ChatRoomParticipantRepository partRepo;
    private final ChatMessageRepository msgRepo;
    private final ChatRoomReadStatusRepository readStatusRepo;
    private final UserClient userClient;
    private final RagClient ragClient;

    @Transactional
    public ChatMessageEntity sendMessage(Long roomId, String senderId, String content) {
        ChatRoomEntity room = isExistChatRoom(roomId);

        // 참여자가 아니면 자동으로 추가
        boolean alreadyParticipant = partRepo.findByRoomIdAndUserId(roomId, senderId)
                .isPresent();

        if (!alreadyParticipant) {
            partRepo.save(ChatRoomParticipantEntity.builder()
                    .room(room)
                    .userId(senderId)
                    .build());
            log.info("User {} automatically joined room {}", senderId, roomId);
        }

        ChatMessageEntity msg = ChatMessageEntity.builder()
                .room(room)
                .senderId(senderId)
                .content(content)
                .sentAt(LocalDateTime.now())
                .build();
        return msgRepo.save(msg);
    }

    @Transactional(readOnly = true)
    public ChatHistoryRes getHistory(Long roomId, Pageable pageable, String userId) {
        Slice<ChatMessageEntity> chatSlice = msgRepo.findByRoomIdOrderBySentAtDesc(roomId, pageable);
        List<ChatMessageEntity> chats = chatSlice.getContent();

        Set<String> senderIds = chats.stream()
                .map(ChatMessageEntity::getSenderId)
                .collect(Collectors.toSet());

        Map<String, String> userIdToNickname = new java.util.HashMap<>();
        if (!senderIds.isEmpty()) {
            List<UserInfo> users = userClient.getUserInfos(new ArrayList<>(senderIds));
            userIdToNickname.putAll(users.stream()
                    .collect(Collectors.toMap(
                            UserInfo::getUserId,
                            UserInfo::getNickname
                    )));
        }

        List<ChatMessageRes> messages = chats.stream()
                .map(chat -> ChatMessageRes.of(
                        chat,
                        userIdToNickname.get(chat.getSenderId())
                ))
                .toList();

        SliceInfo sliceInfo = SliceInfo.of(chatSlice.hasNext());
        return ChatHistoryRes.of(chats, sliceInfo, userId, userIdToNickname);
    }

    @Transactional
    public CreateRoomRes createRoom(boolean groupChat, List<String> userIds) {
        String roomUuid = UUID.randomUUID().toString();

        ChatRoomEntity room = roomRepo.save(ChatRoomEntity.builder()
                .roomUuid(roomUuid)
                .groupChat(groupChat)
                .createdAt(LocalDateTime.now())
                .build());

        userIds.forEach(uid ->
                partRepo.save(ChatRoomParticipantEntity.builder()
                        .room(room)
                        .userId(uid)
                        .build()));

        return new CreateRoomRes(roomUuid);
    }

    @Transactional(readOnly = true)
    public ChatRoomListRes getChatRooms(String userId, Pageable pageable) {
        Page<ChatRoomParticipantEntity> page = partRepo.findByUserId(userId, pageable);

        List<ChatRoomListRes.ChatRoomSummary> roomSummaries = page.getContent().stream()
                .map(participant -> getChatRoomSummary(participant.getRoom().getId(), userId))
                .toList();

        return ChatRoomListRes.builder()
                .rooms(roomSummaries)
                .currentPage(pageable.getPageNumber() + 1)
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    @Transactional
    public void markRoomAsRead(Long roomId, String userId, Long lastReadMessageId) {
        ChatRoomEntity room = isExistChatRoom(roomId);
        ChatRoomReadStatusEntity status = readStatusRepo.findByChatRoomAndUserId(room, userId)
                .orElse(ChatRoomReadStatusEntity.builder()
                        .chatRoom(room)
                        .userId(userId)
                        .lastReadMessageId(lastReadMessageId)
                        .updatedAt(LocalDateTime.now())
                        .build());
        status.updateLastRead(lastReadMessageId);
        readStatusRepo.save(status);
    }

    @Transactional(readOnly = true)
    public ChatRoomListRes.ChatRoomSummary getChatRoomSummary(Long roomId, String userId) {
        ChatRoomEntity room = isExistChatRoom(roomId);

        String opponentId = room.getParticipants().stream()
                .map(ChatRoomParticipantEntity::getUserId)
                .filter(id -> !id.equals(userId))
                .findFirst()
                .orElse(null);

        String opponentNickname = null;
        String opponentProfileUrl = null;

        if (opponentId != null) {
            try {
                UserInfo opponent = userClient.getUserInfo(opponentId);
                if (opponent != null) {
                    opponentNickname = opponent.nickname();
                    opponentProfileUrl = opponent.profileUrl();
                }
            } catch (Exception e) {
                log.warn("Failed to get user info for opponentId: {}, using default nickname", opponentId);
                opponentNickname = "알 수 없음";
            }
        }

        ChatMessageEntity lastMessage = msgRepo.findTopByRoomOrderBySentAtDesc(room);

        ChatRoomReadStatusEntity readStatus = readStatusRepo.findByChatRoomAndUserId(room, userId)
                .orElse(null);

        Long lastReadMessageId = readStatus != null ? readStatus.getLastReadMessageId() : 0L;

        int unreadCount = msgRepo.countByRoomIdAndIdGreaterThanAndSenderIdNot(
                room.getId(), lastReadMessageId, userId);

        return ChatRoomListRes.ChatRoomSummary.builder()
                .id(room.getId())
                .nickname(opponentNickname)
                .lastMessage(lastMessage != null ? lastMessage.getContent() : null)
                .lastMessageTime(lastMessage != null ? lastMessage.getSentAt() : null)
                .unreadCount(unreadCount)
                .profileUrl(opponentProfileUrl)
                .build();
    }

    @Transactional
    public void leaveRoom(Long roomId, String userId) {
        ChatRoomEntity room = isExistChatRoom(roomId);

        ChatRoomParticipantEntity participant = isExistParticipant(roomId, userId);

        partRepo.delete(participant);

        readStatusRepo.findByChatRoomAndUserId(room, userId)
                .ifPresent(readStatusRepo::delete);
    }

    private ChatRoomParticipantEntity isExistParticipant(Long roomId, String userId) {
        return partRepo.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new ApiException("Participant not found"));
    }

    private ChatRoomEntity isExistChatRoom(Long roomId) {
        return roomRepo.findById(roomId)
                .orElseThrow(() -> new ApiException("chat room not found"));
    }

    @Transactional(readOnly = true)
    public List<ChatMessageRes> getAllMessagesByRoomUuid(String roomUuid, String userId) {
        ChatRoomEntity room = roomRepo.findByRoomUuid(roomUuid)
                .orElseThrow(() -> new ApiException("chat room not found"));

        // 초대 링크로 접속한 사용자는 반드시 가입/로그인한 상태이므로 참여자가 아니면 에러
        partRepo.findByRoomIdAndUserId(room.getId(), userId)
                .orElseThrow(() -> new ApiException("user is not a participant of this chat room"));

        List<ChatMessageEntity> messages = msgRepo.findByRoomIdOrderBySentAtAsc(room.getId());

        if (messages.isEmpty()) {
            return List.of();
        }

        Set<String> senderIds = messages.stream()
                .map(ChatMessageEntity::getSenderId)
                .collect(Collectors.toSet());

        Map<String, String> userIdToNickname = new java.util.HashMap<>();
        if (!senderIds.isEmpty()) {
            List<UserInfo> users = userClient.getUserInfos(new ArrayList<>(senderIds));
            userIdToNickname.putAll(users.stream()
                    .collect(Collectors.toMap(
                            UserInfo::getUserId,
                            UserInfo::getNickname
                    )));
        }

        return messages.stream()
                .map(msg -> ChatMessageRes.of(msg, userIdToNickname.get(msg.getSenderId())))
                .toList();
    }

    @Transactional
    public String endChat(Long roomId, String userId) {
        ChatRoomEntity room = isExistChatRoom(roomId);

        // 참여자 검증: 채팅방 참여자만 종료 가능
        partRepo.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new ApiException("user is not a participant of this chat room"));

        String reportId = UUID.randomUUID().toString();

        log.info("Chat ended - roomId: {}, userId: {}, reportId: {}", roomId, userId, reportId);

        asyncRagAnalysis(roomId);

        return reportId;
    }

    @Transactional
    public String endChatByUuid(String roomUuid, String userId) {
        ChatRoomEntity room = roomRepo.findByRoomUuid(roomUuid)
                .orElseThrow(() -> new ApiException("chat room not found"));

        // 초대한 사용자 또는 초대 링크로 합류한 사용자 중 한 명만 종료 가능
        partRepo.findByRoomIdAndUserId(room.getId(), userId)
                .orElseThrow(() -> new ApiException("user is not a participant of this chat room"));

        String reportId = UUID.randomUUID().toString();

        log.info("Chat ended by UUID - roomUuid: {}, reportId: {}", roomUuid, reportId);

        asyncRagAnalysis(room.getId());

        return reportId;
    }

    @Transactional(readOnly = true)
    public Long getRoomIdByUuid(String roomUuid) {
        ChatRoomEntity room = roomRepo.findByRoomUuid(roomUuid)
                .orElseThrow(() -> new ApiException("chat room not found"));
        return room.getId();
    }

    @Transactional
    public CreateRoomRes joinRoomByUuid(String roomUuid, String userId) {
        ChatRoomEntity room = roomRepo.findByRoomUuid(roomUuid)
                .orElseThrow(() -> new ApiException("chat room not found"));

        boolean exists = partRepo.findByRoomIdAndUserId(room.getId(), userId).isPresent();
        if (!exists) {
            partRepo.save(ChatRoomParticipantEntity.builder()
                    .room(room)
                    .userId(userId)
                    .build());
            log.info("User {} joined room {} (uuid={}) via invite link", userId, room.getId(), roomUuid);
        } else {
            log.info("User {} already participates in room {} (uuid={})", userId, room.getId(), roomUuid);
        }

        return new CreateRoomRes(room.getRoomUuid());
    }

    @Async
    public void asyncRagAnalysis(Long roomId) {
        try {
            log.info("Starting async RAG analysis for roomId: {}", roomId);

            List<ChatRoomParticipantEntity> participants = partRepo.findByRoomId(roomId);
            if (participants.size() != 2) {
                log.error("Room {} must have exactly 2 participants for analysis", roomId);
                return;
            }

            String user1Id = participants.get(0).getUserId();
            String user2Id = participants.get(1).getUserId();

            // 채팅 메시지 조회
            List<ChatMessageEntity> messages = msgRepo.findByRoomIdOrderBySentAtAsc(roomId);

            // 사용자 정보 조회
            Set<String> senderIds = messages.stream()
                    .map(ChatMessageEntity::getSenderId)
                    .collect(Collectors.toSet());

            Map<String, String> userIdToName = new java.util.HashMap<>();
            if (!senderIds.isEmpty()) {
                List<UserInfo> users = userClient.getUserInfos(new ArrayList<>(senderIds));
                userIdToName.putAll(users.stream()
                        .collect(Collectors.toMap(
                                UserInfo::getUserId,
                                UserInfo::getNickname
                        )));
            }

            // ChatMessageDto 변환
            List<ChatMessageDto> chatData = messages.stream()
                    .map(msg -> ChatMessageDto.builder()
                            .userId(msg.getSenderId())
                            .name(userIdToName.getOrDefault(msg.getSenderId(), "알 수 없음"))
                            .message(msg.getContent())
                            .build())
                    .toList();

            // RAG 요청 DTO 생성
            RagRequestDto requestDto = RagRequestDto.builder()
                    .user1Id(user1Id)
                    .user2Id(user2Id)
                    .chatData(chatData)
                    .build();

            // RAG 서비스 호출
            ragClient.analyzeConversation(requestDto);

            log.info("RAG analysis completed for roomId: {}", roomId);

        } catch (Exception e) {
            log.error("Failed to analyze conversation for roomId: {}", roomId, e);
        }
    }
}