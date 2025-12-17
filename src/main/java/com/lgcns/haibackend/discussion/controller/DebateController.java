package com.lgcns.haibackend.discussion.controller;

import com.lgcns.haibackend.common.security.AuthUtils;
import com.lgcns.haibackend.discussion.domain.dto.ChatMessage;
import com.lgcns.haibackend.discussion.domain.dto.DebateRoomRequestDTO;
import com.lgcns.haibackend.discussion.domain.dto.DebateRoomResponseDTO;
import com.lgcns.haibackend.discussion.domain.dto.JoinMessage;
import com.lgcns.haibackend.discussion.domain.dto.ChatMessage.MessageType;
import com.lgcns.haibackend.discussion.service.DebateService;
import com.lgcns.haibackend.user.domain.entity.UserEntity;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/debate")
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
            Authentication authentication
    ) {
        return ResponseEntity.ok(
            debateService.getRoomsByClassCode(authentication)
        );
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
        out.setUserId(userId);
        out.setSender(nickname);
        out.setCreatedAt(LocalDateTime.now());

        messagingTemplate.convertAndSend("/topic/room/" + roomId, out);
    }

    @MessageMapping("/chat.sendMessage/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public ChatMessage sendMessage(@DestinationVariable String roomId, @Payload ChatMessage chatMessage) {
        return chatMessage;
    }

    @MessageMapping("/chat.addUser/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public ChatMessage addUser(@DestinationVariable String roomId, @Payload ChatMessage chatMessage,
            SimpMessageHeaderAccessor headerAccessor) {
        // Validate join logic
        debateService.validateJoin(roomId, chatMessage.getUserId());

        // Add username and roomId in web socket session
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        headerAccessor.getSessionAttributes().put("roomId", roomId);
        return chatMessage;
    }
}
