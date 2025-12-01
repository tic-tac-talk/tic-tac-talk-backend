package com.khi.chatservice.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khi.chatservice.application.ChatService;
import com.khi.chatservice.common.annotation.CurrentUser;
import com.khi.chatservice.common.api.ApiResponse;
import com.khi.chatservice.domain.entity.ChatMessageEntity;
import com.khi.chatservice.presentation.dto.SocketEvent;
import com.khi.chatservice.presentation.dto.req.EndChatReq;
import com.khi.chatservice.presentation.dto.req.MarkAsReadReq;
import com.khi.chatservice.presentation.dto.req.MessageReadReq;
import com.khi.chatservice.presentation.dto.req.SendMessageReq;
import com.khi.chatservice.presentation.dto.res.ChatHistoryRes;
import com.khi.chatservice.presentation.dto.res.ChatMessageRes;
import com.khi.chatservice.presentation.dto.res.ChatRoomListRes;
import com.khi.chatservice.presentation.dto.res.CreateRoomRes;
import com.khi.chatservice.presentation.dto.res.EndChatRes;
import com.khi.chatservice.util.EventBroadcaster;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "Chat API", description = """
    채팅 관련 HTTP API

    ## WebSocket 엔드포인트
    - **연결**: /api/v1/chat/ws-chat
    - **프로토콜**: STOMP over WebSocket
    - **인증**: JWT 필요 (Authorization 헤더에 Bearer 토큰)

    ## WebSocket 이벤트 (destination: /app/chat)

    ### 1. SEND_MESSAGE (메시지 전송)
    **Client → Server:**
    ```json
    {
      "type": "SEND_MESSAGE",
      "content": {
        "roomId": "room-1763979187458-yrbrmmj",
        "message": "메시지 내용"
      }
    }
    ```

    ### 2. MESSAGE_READ (읽음 처리)
    **Client → Server:**
    ```json
    {
      "type": "MESSAGE_READ",
      "content": {
        "roomId": "room-1763979187458-yrbrmmj",
        "lastReadMessageId": 456
      }
    }
    ```

    ### 3. CHAT_END (채팅 종료)
    **Client → Server:**
    ```json
    {
      "type": "CHAT_END",
      "content": {
        "roomId": "room-1763979187458-yrbrmmj"
      }
    }
    ```

    ## 구독 토픽 및 수신 이벤트

    ### /topic/room/{roomId}
    채팅방 실시간 이벤트 수신

    #### NEW_MESSAGE (새 메시지)
    ```json
    {
      "type": "NEW_MESSAGE",
      "content": {
        "messageId": 123,
        "senderId": "user123",
        "senderNickname": "홍길동",
        "senderProfileUrl": "https://example.com/profile.jpg",
        "content": "메시지 내용",
        "sentAt": "2024-01-01T12:00:00"
      }
    }
    ```

    #### MESSAGE_READ (읽음 처리)
    ```json
    {
      "type": "MESSAGE_READ",
      "content": 456
    }
    ```

    #### CHAT_END (채팅 종료)
    ```json
    {
      "type": "CHAT_END",
      "content": {
        "message": "채팅이 종료되었습니다.",
        "reportId": "1234567890"
      }
    }
    ```

    #### USER_JOINED (사용자 참여)
    ```json
    {
      "type": "USER_JOINED",
      "content": {
        "userId": "user456",
        "nickname": "김철수",
        "message": "김철수님이 채팅방에 참여했습니다."
      }
    }
    ```

    ### /topic/user-room-updates/{userId}
    사용자별 채팅방 목록 업데이트

    #### CHAT_ROOM_UPDATE
    ```json
    {
      "type": "CHAT_ROOM_UPDATE",
      "content": {
        "id": 1,
        "nickname": "상대방 닉네임",
        "profileUrl": "https://example.com/profile.jpg",
        "lastMessage": "마지막 메시지 내용",
        "lastMessageTime": "2024-01-01T12:00:00",
        "unreadCount": 3
      }
    }
    ```
    """)
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/chat")
public class ChatController {
    
        private final ChatService chatService;
        private final EventBroadcaster eventBroadcaster;
        private final ObjectMapper objectMapper;

        @MessageMapping("/chat")
        public void handleChatEvent(@Payload SocketEvent<?> event, SimpMessageHeaderAccessor accessor) {
            String userId = (String) accessor.getSessionAttributes().get("userId");
            log.info("convertAndSend userId: {}", userId);

            switch (event.type()) {
                case SEND_MESSAGE -> {
                    SendMessageReq req = convert(event.content(), SendMessageReq.class);
                    String roomUuid = req.roomId();
                    Long roomId = chatService.getRoomIdByUuid(roomUuid);
                    ChatMessageEntity savedMsg = chatService.sendMessage(roomId, userId, req.message());
                    eventBroadcaster.broadcastNewMessage(savedMsg, userId);
                }
                case MESSAGE_READ -> {
                    MessageReadReq req = convert(event.content(), MessageReadReq.class);
                    String roomUuid = req.roomId();
                    Long roomId = chatService.getRoomIdByUuid(roomUuid);
                    chatService.markRoomAsRead(roomId, userId, req.lastReadMessageId());
                    eventBroadcaster.broadcastMessageRead(roomUuid, req.lastReadMessageId());
                }
                case CHAT_END -> {
                    EndChatReq req = convert(event.content(), EndChatReq.class);
                    String roomUuid = req.roomId();
                    String reportId = chatService.endChatByUuid(roomUuid, userId);
                    eventBroadcaster.broadcastChatEndToAll(roomUuid, reportId);
                    log.info("Chat ended via WebSocket - roomId(uuid): {}, userId: {}, reportId: {}", roomUuid, userId, reportId);
                }
            }
        }

        @Operation(summary = "채팅방 생성", description = "상대 사용자와 1:1 채팅방을 생성합니다.")
        @PostMapping("/room")
        public ApiResponse<CreateRoomRes> makeRoom(
                // @CurrentUser String userId
                @RequestHeader("X-User-Id") String userId
        ){
            log.info("[CHAT-SERVICE] Received X-User-Id header: {} for POST /room", userId);
            List<String> userIds = new ArrayList<>();
            userIds.add(userId);

            CreateRoomRes res = chatService.createRoom(false, userIds);

            return ApiResponse.success(res);
        }

//    @Operation(summary = "채팅방 메시지 조회", description = "채팅방의 메시지 목록을 페이지네이션으로 조회합니다.")
//    @GetMapping("/rooms/{roomId}/messages")
//        public ApiResponse<ChatHistoryRes> getMessages(
//                @CurrentUser String userId,
//                @PathVariable Long roomId,
//                @PageableDefault Pageable pageable
//        ) {
//            ChatHistoryRes slice = chatService.getHistory(roomId, pageable, userId);
//            return ApiResponse.success(slice);
//        }

        @Operation(summary = "채팅방 읽음 처리", description = "지정한 메시지까지 읽음 처리합니다.")
        @PutMapping("/rooms/{roomId}/read")
        public ApiResponse<?> markRoomAsRead(
                // @CurrentUser String userId,
                @RequestHeader("X-User-Id") String userId,
                @PathVariable String roomId,
                @RequestBody MarkAsReadReq request
        ) {
            log.info("[CHAT-SERVICE] Received X-User-Id header: {} for PUT /rooms/{}/read", userId, roomId);
            Long roomIdLong = chatService.getRoomIdByUuid(roomId);
            chatService.markRoomAsRead(roomIdLong, userId, request.lastReadMessageId());
            eventBroadcaster.broadcastMessageRead(roomId, request.lastReadMessageId());
            return ApiResponse.success();
        }

//    @Operation(summary = "채팅방 나가기", description = "채팅방에서 사용자를 제거합니다.")
//    @DeleteMapping("/rooms/{roomId}")
//        public ApiResponse<?> leaveRoom(
//                @CurrentUser String userId,
//                @PathVariable Long roomId
//        ) {
//            chatService.leaveRoom(roomId, userId);
//            return ApiResponse.success();
//        }

        @Operation(summary = "UUID로 채팅 기록 전체 조회", description = "사용자가 roomUuid를 기준으로 전체 메시지를 조회합니다.")
        @GetMapping("/rooms/{roomId}/messages")
        public ApiResponse<List<ChatMessageRes>> getAllMessagesByRoomUuid(
                @PathVariable String roomId,
                // @CurrentUser String userId
                @RequestHeader("X-User-Id") String userId
        ) {
            log.info("[CHAT-SERVICE] Received X-User-Id header: {} for GET /rooms/{}/messages", userId, roomId);
            String roomUuid = roomId;
            return ApiResponse.success(chatService.getAllMessagesByRoomUuid(roomUuid, userId));
        }

        @Operation(summary = "초대 링크 참가", description = "roomId를 통해 사용자를 채팅방에 참여시킵니다.")
        @PostMapping("/rooms/{roomId}/join")
        public ApiResponse<CreateRoomRes> joinRoomByUuid(
                // @CurrentUser String userId,
                @RequestHeader("X-User-Id") String userId,
                @PathVariable String roomId
        ) {
            log.info("[CHAT-SERVICE] Received X-User-Id header: {} for POST /rooms/{}/join", userId, roomId);
            CreateRoomRes res = chatService.joinRoomByUuid(roomId, userId);
            eventBroadcaster.broadcastUserJoined(roomId, userId);
            return ApiResponse.success(res);
        }

        @Operation(summary = "채팅 종료", description = "사용자가 초대 링크(roomId) 기준으로 채팅을 종료합니다.")
        @PostMapping("/rooms/{roomId}/end")
        public ApiResponse<EndChatRes> endChatByUuid(
                @PathVariable String roomId,
                // @CurrentUser String userId
                @RequestHeader("X-User-Id") String userId
        ) {
            log.info("[CHAT-SERVICE] Received X-User-Id header: {} for POST /rooms/{}/end", userId, roomId);
            String reportId = chatService.endChatByUuid(roomId, userId);
            eventBroadcaster.broadcastChatEndToAll(roomId, reportId);
            return ApiResponse.success(new EndChatRes(reportId));
        }

        private <T> T convert(Object content, Class<T> clazz) {
            return objectMapper.convertValue(content, clazz);
        }
    }
