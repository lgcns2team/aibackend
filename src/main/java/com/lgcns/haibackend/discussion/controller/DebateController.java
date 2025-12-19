package com.lgcns.haibackend.discussion.controller;

import com.lgcns.haibackend.common.security.AuthUtils;
import com.lgcns.haibackend.discussion.domain.dto.ChatMessage;
import com.lgcns.haibackend.discussion.domain.dto.DebateRoomRequestDTO;
import com.lgcns.haibackend.discussion.domain.dto.DebateRoomResponseDTO;
import com.lgcns.haibackend.discussion.domain.dto.DebateStatus;
import com.lgcns.haibackend.discussion.domain.dto.StatusSelectMessage;
import com.lgcns.haibackend.discussion.service.DebateService;

import lombok.RequiredArgsConstructor;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/debate")
public class DebateController {

    private final DebateService debateService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/room")
    public ResponseEntity<DebateRoomResponseDTO> createRoom(@RequestBody DebateRoomRequestDTO req,
            Authentication authentication) {
        DebateRoomResponseDTO room = debateService.createRoom(req, authentication);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/roomList")
    public ResponseEntity<List<DebateRoomResponseDTO>> getRoomsByTeacher(
            Authentication authentication) {
        return ResponseEntity.ok(
                debateService.getRoomsByClassCode(authentication));
    }

    @MessageMapping("/room/{roomId}/join")
    public void join(
        @DestinationVariable String roomId,
        SimpMessageHeaderAccessor headerAccessor,
        Principal principal
    ) {
        UUID userId = AuthUtils.getUserId(principal);
        debateService.validateJoin(roomId, userId);
        String nickname = debateService.getNickName(userId);

        Map<String, Object> session = headerAccessor.getSessionAttributes();
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No websocket session");
        }
        session.put("roomId", roomId);
        session.put("userId", userId.toString());
        session.put("sender", nickname);

        ChatMessage out = new ChatMessage();
        out.setType(ChatMessage.MessageType.JOIN);
        out.setSender(nickname);
        out.setCreatedAt(LocalDateTime.now());

        messagingTemplate.convertAndSend("/topic/room/" + roomId, out);
    }

    @MessageMapping("/room/{roomId}/status")
    public void selectStatus(
        @DestinationVariable String roomId,
        @Payload StatusSelectMessage msg,
        SimpMessageHeaderAccessor headerAccessor,
        Principal principal
    ) {
        UUID userId = AuthUtils.getUserId(principal);

        debateService.validateJoin(roomId, userId);

        // status 저장
        debateService.saveStatus(roomId, userId, msg.getStatus());

        // 세션에 저장
        Map<String, Object> session = headerAccessor.getSessionAttributes();
        if (session != null) {
            session.put("status", msg.getStatus().name());
        }

        String nickname = (String) (session != null ? session.get("sender") : null);
        if (nickname == null) nickname = debateService.getNickName(userId);

        ChatMessage out = ChatMessage.builder()
            .type(ChatMessage.MessageType.STATUS)
            .sender(nickname)
            .status(msg.getStatus())
            .createdAt(LocalDateTime.now())
            .build();

        messagingTemplate.convertAndSend("/topic/room/" + roomId, out);
    }

    @MessageMapping("/room/{roomId}/chat")
    public void sendMessage(
        @DestinationVariable String roomId,
        @Payload ChatMessage incoming,
        SimpMessageHeaderAccessor headerAccessor,
        Principal principal
    ) {
        UUID userId = AuthUtils.getUserId(principal);

        debateService.validateJoin(roomId, userId);

        // status 선택 여부 확인
        DebateStatus status = debateService.requireStatusSelected(roomId, userId, headerAccessor);
        String sender = debateService.resolveNickname(userId, headerAccessor);

        ChatMessage out = ChatMessage.builder()
            .type(ChatMessage.MessageType.CHAT)
            .content(incoming.getContent())
            .sender(sender)
            .status(status)
            .createdAt(LocalDateTime.now())
            .build();

        debateService.appendMessage(roomId, out);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, out);
    }

    // @MessageMapping("/chat.sendMessage/{roomId}")
    // @SendTo("/topic/room/{roomId}")
    // public ChatMessage sendMessage(@DestinationVariable String roomId, @Payload ChatMessage chatMessage) {
    //     return chatMessage;
    // }

    // @MessageMapping("/chat.addUser/{roomId}")
    // @SendTo("/topic/room/{roomId}")
    // public ChatMessage addUser(@DestinationVariable String roomId, @Payload ChatMessage chatMessage,
    //         SimpMessageHeaderAccessor headerAccessor) {
    //     // Validate join logic
    //     debateService.validateJoin(roomId, chatMessage.getUserId());

    //     // Add username and roomId in web socket session
    //     headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
    //     headerAccessor.getSessionAttributes().put("roomId", roomId);
    //     return chatMessage;
    // }

    @DeleteMapping("/room/{roomId}")
    public ResponseEntity<Void> deleteRoom(@RequestParam Integer teacherCode, @PathVariable UUID roomId, Authentication authentication) {
        debateService.deleteRoom(teacherCode, roomId.toString(), authentication);

        messagingTemplate.convertAndSend("/sub/chat/room/" + roomId,
            Map.of("type", "ROOM_DELETED", "message", "토론이 종료되었습니다."));

        return ResponseEntity.ok().build();
    }
    /**
     * 토론 주제 추천 API
     * AWS Bedrock Prompt를 통해 한국 역사 토론 주제를 추천받습니다.
     */
    @PostMapping("/topics/recommend")
    public ResponseEntity<com.lgcns.haibackend.discussion.domain.dto.DebateTopicsResponse> recommendTopics(
            @RequestBody com.lgcns.haibackend.discussion.domain.dto.DebateTopicsRequest request) {
        com.lgcns.haibackend.discussion.domain.dto.DebateTopicsResponse response = debateService
                .getDebateTopicRecommendations(request);
        return ResponseEntity.ok(response);
    }
}
