package com.khi.chatservice.application;

import com.khi.chatservice.client.UserClient;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Transactional
    public ChatMessageEntity sendMessage(Long roomId, String senderId, String content) {
        ChatRoomEntity room = isExistChatRoom(roomId);

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

        List<UserInfo> users = userClient.getUserInfos(new ArrayList<>(senderIds));

        Map<String, String> userIdToNickname = users.stream()
                .collect(Collectors.toMap(
                        UserInfo::getUserId,
                        UserInfo::getNickname
                ));

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

        ChatRoomEntity room = roomRepo.save(ChatRoomEntity.builder()
                .groupChat(groupChat)
                .createdAt(LocalDateTime.now())
                .build());

        userIds.forEach(uid ->
                partRepo.save(ChatRoomParticipantEntity.builder()
                        .room(room)
                        .userId(uid)
                        .build()));

        return new CreateRoomRes(room.getId());
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

        UserInfo opponent = opponentId != null
                ? userClient.getUserInfo(opponentId)
                : null;

        ChatMessageEntity lastMessage = msgRepo.findTopByRoomOrderBySentAtDesc(room);

        ChatRoomReadStatusEntity readStatus = readStatusRepo.findByChatRoomAndUserId(room, userId)
                .orElse(null);

        Long lastReadMessageId = readStatus != null ? readStatus.getLastReadMessageId() : 0L;

        int unreadCount = msgRepo.countByRoomIdAndIdGreaterThanAndSenderIdNot(
                room.getId(), lastReadMessageId, userId);

        return ChatRoomListRes.ChatRoomSummary.builder()
                .id(room.getId())
                .nickname(opponent != null ? opponent.nickname() : null)
                .lastMessage(lastMessage != null ? lastMessage.getContent() : null)
                .lastMessageTime(lastMessage != null ? lastMessage.getSentAt() : null)
                .unreadCount(unreadCount)
                .profileUrl(opponent != null ? opponent.profileUrl() : null)
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
}