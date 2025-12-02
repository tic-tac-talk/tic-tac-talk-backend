package com.khi.chatservice.application;

import com.khi.chatservice.client.RagClient;
import com.khi.chatservice.client.UserClient;
import com.khi.chatservice.client.dto.ChatMessageDto;
import com.khi.chatservice.client.dto.ChatRagRequestDto;
import com.khi.chatservice.client.dto.UserInfo;
import com.khi.chatservice.domain.entity.ChatMessageEntity;
import com.khi.chatservice.domain.entity.ChatRoomEntity;
import com.khi.chatservice.domain.entity.ChatRoomParticipantEntity;
import com.khi.chatservice.domain.repository.ChatMessageRepository;
import com.khi.chatservice.domain.repository.ChatRoomParticipantRepository;
import com.khi.chatservice.domain.repository.ChatRoomReadStatusRepository;
import com.khi.chatservice.domain.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatAnalysisService {

    private final ChatRoomParticipantRepository partRepo;
    private final ChatMessageRepository msgRepo;
    private final UserClient userClient;
    private final RagClient ragClient;
    private final ChatRoomRepository roomRepo;
    private final ChatRoomReadStatusRepository readStatusRepo;

    @Async
    @Transactional
    public void asyncRagAnalysis(Long roomId, Long reportId) {
        try {
            log.info("Starting async RAG analysis for roomId: {}, reportId: {}", roomId, reportId);

            // 채팅방 조회하여 creatorId 가져오기
            ChatRoomEntity room = roomRepo.findById(roomId)
                    .orElseThrow(() -> new RuntimeException("Chat room not found: " + roomId));

            String creatorId = room.getCreatorId();
            if (creatorId == null) {
                log.error("Room {} has no creator", roomId);
                return;
            }

            List<ChatRoomParticipantEntity> participants = partRepo.findByRoomId(roomId);
            if (participants.size() != 2) {
                log.error("Room {} must have exactly 2 participants for analysis", roomId);
                return;
            }

            // user1Id는 방 개설자(creator), user2Id는 나머지 참여자
            String user1Id = creatorId;
            String user2Id = participants.stream()
                    .map(ChatRoomParticipantEntity::getUserId)
                    .filter(id -> !id.equals(creatorId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Could not find other participant"));

            log.info("Room {} - creator(user1): {}, other(user2): {}", roomId, user1Id, user2Id);

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

            // RAG 요청 DTO 생성 (reportId 포함)
            ChatRagRequestDto requestDto = ChatRagRequestDto.builder()
                    .reportId(reportId)
                    .user1Id(user1Id)
                    .user2Id(user2Id)
                    .chatData(chatData)
                    .build();

            // RAG 서비스 호출 (reportId 포함한 새로운 엔드포인트)
            ragClient.analyzeChatConversationWithReportId(requestDto);

            log.info("RAG analysis completed for roomId: {}, reportId: {}", roomId, reportId);

        } catch (Exception e) {
            log.error("Failed to analyze conversation for roomId: {}, reportId: {}", roomId, reportId, e);
        }
    }
}
