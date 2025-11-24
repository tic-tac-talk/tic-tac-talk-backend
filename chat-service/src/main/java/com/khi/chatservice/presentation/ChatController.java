package com.khi.chatservice.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khi.chatservice.application.ChatService;
import com.khi.chatservice.common.annotation.CurrentUser;
import com.khi.chatservice.common.api.ApiResponse;
import com.khi.chatservice.domain.entity.ChatMessageEntity;
import com.khi.chatservice.domain.entity.ChatRoomEntity;
import com.khi.chatservice.presentation.dto.SocketEvent;
import com.khi.chatservice.presentation.dto.req.MarkAsReadReq;
import com.khi.chatservice.presentation.dto.req.MessageReadReq;
import com.khi.chatservice.presentation.dto.req.SendMessageReq;
import com.khi.chatservice.presentation.dto.res.ChatHistoryRes;
import com.khi.chatservice.presentation.dto.res.ChatRoomListRes;
import com.khi.chatservice.presentation.dto.res.CreateRoomRes;
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

@Tag(name = "Chat API", description = "채팅 관련 HTTP API")
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
                    ChatMessageEntity savedMsg = chatService.sendMessage(req.roomId(), userId, req.message());
                    eventBroadcaster.broadcastNewMessage(savedMsg, userId);
                }
                case MESSAGE_READ -> {
                    MessageReadReq req = convert(event.content(), MessageReadReq.class);
                    chatService.markRoomAsRead(req.roomId(), userId, req.lastReadMessageId());
                    eventBroadcaster.broadcastMessageRead(req.roomId(), req.lastReadMessageId());
                }
            }
        }

    @Operation(summary = "채팅방 생성", description = "상대 사용자와 1:1 채팅방을 생성합니다.")
    @PostMapping("/room")
        public ApiResponse<?> makeRoom(
                @CurrentUser String userId
        ){
            List<String> userIds = new ArrayList<>();
            userIds.add(userId);

            CreateRoomRes res = chatService.createRoom(false, userIds);

            return ApiResponse.success(res);
        }

    @Operation(summary = "채팅방 메시지 조회", description = "채팅방의 메시지 목록을 페이지네이션으로 조회합니다.")
    @GetMapping("/rooms/{chatRoomId}/messages")
        public ApiResponse<?> getMessages(
                @CurrentUser String userId,
                @PathVariable Long chatRoomId,
                @PageableDefault Pageable pageable
        ) {
            ChatHistoryRes slice = chatService.getHistory(chatRoomId, pageable, userId);
            return ApiResponse.success(slice);
        }

    @Operation(summary = "채팅방 읽음 처리", description = "지정한 메시지까지 읽음 처리합니다.")
    @PutMapping("/rooms/{chatRoomId}/read")
        public ApiResponse<?> markRoomAsRead(
                @CurrentUser String userId,
                @PathVariable Long chatRoomId,
                @RequestBody MarkAsReadReq request
        ) {
            chatService.markRoomAsRead(chatRoomId, userId, request.lastReadMessageId());
            eventBroadcaster.broadcastMessageRead(chatRoomId, request.lastReadMessageId());
            return ApiResponse.success();
        }

    @Operation(summary = "채팅방 나가기", description = "채팅방에서 사용자를 제거합니다.")
    @DeleteMapping("/rooms/{chatRoomId}")
        public ApiResponse<?> leaveRoom(
                @CurrentUser String userId,
                @PathVariable Long chatRoomId
        ) {
            chatService.leaveRoom(chatRoomId, userId);
            return ApiResponse.success();
        }

    @Operation(summary = "UUID로 채팅방 참여", description = "UUID를 통해 채팅방에 참여합니다. 익명 사용자의 Uid는 anonymousId를 생성해 전달합니다.")
    @PostMapping("/rooms/join/{roomUuid}")
        public ApiResponse<?> joinRoomByUuid(
                @PathVariable String roomUuid,
                @RequestParam String anonymousId
        ) {
            ChatRoomEntity room = chatService.joinRoomByUuid(roomUuid, anonymousId);
            return ApiResponse.success(new CreateRoomRes(room.getId(), room.getRoomUuid()));
        }

    @Operation(summary = "UUID로 채팅 기록 전체 조회", description = "UUID로 채팅방의 전체 메시지를 조회합니다. userId 파라미터로 사용자 식별합니다.")
    @GetMapping("/rooms/uuid/{roomUuid}/messages")
        public ApiResponse<?> getAllMessagesByRoomUuid(
                @PathVariable String roomUuid,
                @RequestParam String userId
        ) {
            return ApiResponse.success(chatService.getAllMessagesByRoomUuid(roomUuid, userId));
        }

        private <T> T convert(Object content, Class<T> clazz) {
            return objectMapper.convertValue(content, clazz);
        }
    }
