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

    @org.springframework.web.bind.annotation.DeleteMapping("/room/{roomId}")
    public ResponseEntity<Void> deleteRoom(
            @org.springframework.web.bind.annotation.PathVariable("roomId") String roomId,
            Authentication authentication) {
        debateService.deleteRoom(roomId, authentication);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/roomList")
    public ResponseEntity<List<DebateRoomResponseDTO>> getRoomsByTeacher(
            Authentication authentication,
            @RequestParam(required = false) UUID userId) {
        return ResponseEntity.ok(
                debateService.getRoomsByClassCode(authentication, userId));
    }

    @GetMapping("/room/{roomId}/messages")
    public ResponseEntity<List<ChatMessage>> getRoomMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication auth
    ) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        UUID userId = AuthUtils.getUserId(auth);
        debateService.validateJoin(roomId, userId);

        return ResponseEntity.ok(debateService.getMessages(roomId, page, size));
    }


    @MessageMapping("/room/{roomId}/join")
    public void join(
            @DestinationVariable String roomId,
            @Payload Map<String, String> payload,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {
        UUID userId = null;
        if (payload != null && payload.containsKey("userId")) {
            try {
                userId = UUID.fromString(payload.get("userId"));
            } catch (Exception e) {
                // Ignore invalid UUID
            }
        }
        if (userId == null) {
            userId = AuthUtils.getUserId(principal);
        }

        debateService.validateJoin(roomId, userId);
        String nickname = debateService.getNickName(userId);

        // If nickname is null (Guest), try to get from payload
        if (nickname == null && payload != null && payload.containsKey("sender")) {
            nickname = payload.get("sender");
        }
        if (nickname == null)
            nickname = "Guest";

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
            Principal principal) {
        UUID userId = msg.getUserId();

        if (userId == null) {
            Map<String, Object> session = headerAccessor.getSessionAttributes();
            if (session != null && session.containsKey("userId")) {
                try {
                    userId = UUID.fromString((String) session.get("userId"));
                } catch (Exception e) {
                }
            }
        }

        if (userId == null) {
            try {
                userId = AuthUtils.getUserId(principal);
            } catch (Exception e) {
                // Ignore auth error if we just want to fail later or handle guests
            }
        }

        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User ID not found");
        }

        debateService.validateJoin(roomId, userId);

        // status 저장
        debateService.saveStatus(roomId, userId, msg.getStatus());

        if (headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("status", msg.getStatus().name());
        }

        String nickname = (String) (headerAccessor.getSessionAttributes() != null
                ? headerAccessor.getSessionAttributes().get("sender")
                : null);
        if (nickname == null)
            nickname = debateService.getNickName(userId);

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
            Principal principal) {
        UUID userId = incoming.getUserId();

        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User ID not found");
        }

        debateService.validateJoin(roomId, userId);

        // status 선택 여부 확인
        DebateStatus status = debateService.requireStatusSelected(roomId, userId, headerAccessor);
        String sender = debateService.resolveNickname(userId, headerAccessor);

        ChatMessage out = ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .parentId(incoming.getParentId())
                .userId(userId)
                .type(ChatMessage.MessageType.CHAT)
                .content(incoming.getContent())
                .sender(sender)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();

        debateService.appendMessage(roomId, out);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, out);
    }

    /**
     * 토론 주제 추천 API
     * AWS Bedrock Prompt를 통해 한국 역사 토론 주제를 추천받습니다.
     */
    @PostMapping("/topics/recommend")

    @MessageMapping("/room/{roomId}/mode")
    public void updateMode(
            @DestinationVariable String roomId,
            @Payload Map<String, String> payload,
            Principal principal) {
        // Teacher validation could be added here
        String newMode = payload.get("viewMode");
        if (newMode == null)
            return;

        debateService.updateRoomMode(roomId, newMode);

        ChatMessage out = ChatMessage.builder()
                .type(ChatMessage.MessageType.MODE_CHANGE)
                .content(newMode)
                .createdAt(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/room/" + roomId, out);
    }

    public ResponseEntity<com.lgcns.haibackend.discussion.domain.dto.DebateTopicsResponse> recommendTopics(
            @RequestBody com.lgcns.haibackend.discussion.domain.dto.DebateTopicsRequest request) {
        com.lgcns.haibackend.discussion.domain.dto.DebateTopicsResponse response = debateService
                .getDebateTopicRecommendations(request);
        return ResponseEntity.ok(response);
    }
}
