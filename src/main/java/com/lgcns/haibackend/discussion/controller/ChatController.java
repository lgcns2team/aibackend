package com.lgcns.haibackend.discussion.controller;

import com.lgcns.haibackend.discussion.domain.dto.ChatMessage;
import com.lgcns.haibackend.discussion.domain.dto.Room;
import com.lgcns.haibackend.discussion.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/chat/room")
    public ResponseEntity<Room> createRoom(@RequestBody Map<String, String> payload) {
        String teacherIdStr = payload.get("teacherId");
        UUID teacherId = UUID.fromString(teacherIdStr);
        Room room = chatService.createRoom(teacherId);
        return ResponseEntity.ok(room);
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
        chatService.validateJoin(roomId, chatMessage.getUserId());

        // Add username and roomId in web socket session
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        headerAccessor.getSessionAttributes().put("roomId", roomId);
        return chatMessage;
    }
}
