    package com.khi.chatservice.presentation;

    import com.fasterxml.jackson.databind.ObjectMapper;
    import com.khi.chatservice.application.ChatService;
    import com.khi.chatservice.common.annotation.CurrentUser;
    import com.khi.chatservice.common.api.ApiResponse;
    import com.khi.chatservice.domain.entity.ChatMessageEntity;
    import com.khi.chatservice.presentation.dto.SocketEvent;
    import com.khi.chatservice.presentation.dto.req.MarkAsReadReq;
    import com.khi.chatservice.presentation.dto.req.MessageReadReq;
    import com.khi.chatservice.presentation.dto.req.SendMessageReq;
    import com.khi.chatservice.presentation.dto.res.ChatHistoryRes;
    import com.khi.chatservice.presentation.dto.res.ChatRoomListRes;
    import com.khi.chatservice.presentation.dto.res.CreateRoomRes;
    import com.khi.chatservice.util.EventBroadcaster;
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

    @Slf4j
    @RequiredArgsConstructor
    @RestController
    @RequestMapping("/api/chat")
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
    
        @PostMapping("/room")
        public ApiResponse<?> makeRoom(
                @CurrentUser String userId,
                @RequestParam Long otherId
        ){
            List<String> userIds = new ArrayList<>();
            userIds.add(Long.toString(otherId));
            userIds.add(userId);

            CreateRoomRes res = chatService.createRoom(false, userIds);
    
            return ApiResponse.success(res);
        }

        @GetMapping("/rooms/{chatRoomId}/messages")
        public ApiResponse<?> getMessages(
                @CurrentUser String userId,
                @PathVariable Long chatRoomId,
                @PageableDefault Pageable pageable
        ) {
            ChatHistoryRes slice = chatService.getHistory(chatRoomId, pageable, userId);
            return ApiResponse.success(slice);
        }
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

        @GetMapping("/rooms")
        public ApiResponse<?> getChatRooms(
                @CurrentUser String userId,
                @PageableDefault Pageable pageable
        ) {
            ChatRoomListRes res = chatService.getChatRooms(userId, pageable);
            return ApiResponse.success(res);
        }

        @DeleteMapping("/rooms/{chatRoomId}")
        public ApiResponse<?> leaveRoom(
                @CurrentUser String userId,
                @PathVariable Long chatRoomId
        ) {
            chatService.leaveRoom(chatRoomId, userId);
            return ApiResponse.success();
        }

        private <T> T convert(Object content, Class<T> clazz) {
            return objectMapper.convertValue(content, clazz);
        }
    }
