package com.lgcns.haibackend.discussion.controller;

import com.lgcns.haibackend.discussion.domain.dto.ChatMessage;
import com.lgcns.haibackend.discussion.domain.dto.DebateRoomRequestDTO;
import com.lgcns.haibackend.discussion.domain.dto.DebateRoomResponseDTO;
import com.lgcns.haibackend.discussion.service.DebateService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/debate")
public class DebateController {

    private final DebateService debateService;

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
