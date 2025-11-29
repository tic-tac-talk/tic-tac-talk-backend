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
    ```json
    {
      "type": "CHAT_END",
      "content": {
        "roomId": "room-1763979187458-yrbrmmj"
      }
    }
    ```

    ## 구독 토픽
    - **/topic/room/{roomId}**: 채팅방 실시간 이벤트 수신 (NEW_MESSAGE, MESSAGE_READ, CHAT_END)
    - **/topic/user-room-updates/{userId}**: 사용자별 채팅방 목록 업데이트
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
                    Long roomId = chatService.getRoomIdByUuid(req.roomId());
                    ChatMessageEntity savedMsg = chatService.sendMessage(roomId, userId, req.message());
                    eventBroadcaster.broadcastNewMessage(savedMsg, userId);
                }
                case MESSAGE_READ -> {
                    MessageReadReq req = convert(event.content(), MessageReadReq.class);
                    Long roomId = chatService.getRoomIdByUuid(req.roomId());
                    chatService.markRoomAsRead(roomId, userId, req.lastReadMessageId());
                    eventBroadcaster.broadcastMessageRead(roomId, req.lastReadMessageId());
                }
                case CHAT_END -> {
                    EndChatReq req = convert(event.content(), EndChatReq.class);
                    String reportId = chatService.endChatByUuid(req.roomId(), userId);
                    Long roomId = chatService.getRoomIdByUuid(req.roomId());
                    eventBroadcaster.broadcastChatEndToAll(roomId, reportId);
                    log.info("Chat ended via WebSocket - roomId(uuid): {}, userId: {}, reportId: {}", req.roomId(), userId, reportId);
                }
            }
        }

    @Operation(summary = "채팅방 생성", description = "상대 사용자와 1:1 채팅방을 생성합니다.")
    @PostMapping("/room")
        public ApiResponse<CreateRoomRes> makeRoom(
                @CurrentUser String userId
        ){
            List<String> userIds = new ArrayList<>();
            userIds.add(userId);

            CreateRoomRes res = chatService.createRoom(false, userIds);

            return ApiResponse.success(res);
        }

    @Operation(summary = "채팅방 메시지 조회", description = "채팅방의 메시지 목록을 페이지네이션으로 조회합니다.")
    @GetMapping("/rooms/{roomId}/messages")
        public ApiResponse<ChatHistoryRes> getMessages(
                @CurrentUser String userId,
                @PathVariable Long roomId,
                @PageableDefault Pageable pageable
        ) {
            ChatHistoryRes slice = chatService.getHistory(roomId, pageable, userId);
            return ApiResponse.success(slice);
        }

    @Operation(summary = "채팅방 읽음 처리", description = "지정한 메시지까지 읽음 처리합니다.")
    @PutMapping("/rooms/{roomId}/read")
        public ApiResponse<?> markRoomAsRead(
                @CurrentUser String userId,
                @PathVariable Long roomId,
                @RequestBody MarkAsReadReq request
        ) {
            chatService.markRoomAsRead(roomId, userId, request.lastReadMessageId());
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
                @CurrentUser String userId
        ) {
            String roomUuid = roomId;
            return ApiResponse.success(chatService.getAllMessagesByRoomUuid(roomUuid, userId));
        }

    @Operation(summary = "초대 링크 참가", description = "roomUuid를 통해 사용자를 채팅방에 참여시킵니다.")
    @PostMapping("/rooms/{roomId}/join")
        public ApiResponse<CreateRoomRes> joinRoomByUuid(
                @CurrentUser String userId,
                @PathVariable String roomUuid
        ) {
            CreateRoomRes res = chatService.joinRoomByUuid(roomUuid, userId);
            Long roomId = chatService.getRoomIdByUuid(roomUuid);
            eventBroadcaster.broadcastUserJoined(roomId, userId);
            return ApiResponse.success(res);
        }

    @Operation(summary = "채팅 종료", description = "사용자가 초대 링크(roomUuid) 기준으로 채팅을 종료합니다.")
    @PostMapping("/rooms/{roomUuid}/end")
        public ApiResponse<EndChatRes> endChatByUuid(
                @PathVariable String roomUuid,
                @CurrentUser String userId
        ) {
            Long roomId = chatService.getRoomIdByUuid(roomUuid);
            String reportId = chatService.endChatByUuid(roomUuid, userId);
            eventBroadcaster.broadcastChatEndToAll(roomId, reportId);
            return ApiResponse.success(new EndChatRes(reportId));
        }

        private <T> T convert(Object content, Class<T> clazz) {
            return objectMapper.convertValue(content, clazz);
        }
    }
