package com.lgcns.haibackend.discussion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcns.haibackend.common.security.AuthUtils;
import com.lgcns.haibackend.discussion.domain.dto.ChatMessage;
import com.lgcns.haibackend.discussion.domain.dto.DebateRoomRequestDTO;
import com.lgcns.haibackend.discussion.domain.dto.DebateRoomResponseDTO;
import com.lgcns.haibackend.discussion.domain.dto.DebateStatus;
import com.lgcns.haibackend.discussion.domain.dto.DebateTopicsRequest;
import com.lgcns.haibackend.discussion.domain.dto.DebateTopicsResponse;
import com.lgcns.haibackend.user.domain.entity.UserClassInfo;
import com.lgcns.haibackend.user.domain.entity.UserEntity;
import com.lgcns.haibackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class DebateService {

    @Value("${aws.bedrock.prompt.debate-topic}")
    private String debateTopicPromptId;

    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final com.lgcns.haibackend.bedrock.client.FastApiClient fastApiClient;
    private final ObjectMapper objectMapper;
    private final Map<UUID, DebateRoomRequestDTO> activeRooms = new ConcurrentHashMap<>();

    public boolean validateTeacher(Authentication auth) {
        if (auth == null || !AuthUtils.hasRole(auth, "TEACHER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "선생님만 접근할 수 있습니다.");
        }
        return true; // 검증 통과 시 true 반환
    }

    public DebateRoomResponseDTO createRoom(DebateRoomRequestDTO req, Authentication auth) {

        validateTeacher(auth);
        
        UUID teacherId = AuthUtils.getUserId(auth);
        UserEntity teacher = userRepository.findByUserId(teacherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Integer teacherCode = teacher.getTeacherCode();
        UUID roomId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        String roomKey = "debate:room:" + roomId;
        Map<String, String> roomMap = new HashMap<>();
        roomMap.put("roomId", roomId.toString());
        roomMap.put("teacherId", teacherId.toString());
        roomMap.put("teacherCode", teacherCode.toString());
        roomMap.put("grade", req.getGrade().toString());
        roomMap.put("classroom", req.getClassroom().toString());
        roomMap.put("topicTitle", req.getTopicTitle());
        roomMap.put("topicDescription", req.getTopicDescription());
        roomMap.put("participantCount", req.getParticipantCount().toString());
        roomMap.put("createdAt", createdAt.toString());
        roomMap.put("viewMode", "vote");

        redisTemplate.opsForHash().putAll(roomKey, roomMap);

        // 코드별 토론방
        String teacherCodeIndexKey = "debate:teacherCode:" + teacherCode + ":rooms";
        redisTemplate.opsForZSet().add(
                teacherCodeIndexKey,
                roomId.toString(),
                createdAt.atZone(ZoneId.systemDefault()).toEpochSecond());

        return DebateRoomResponseDTO.builder()
                .roomId(roomId)
                .teacherId(teacherId)
                .teacherCode(teacherCode)
                .participantCount(req.getParticipantCount())
                .topicTitle(req.getTopicTitle())
                .topicDescription(req.getTopicDescription())
                .createdAt(createdAt)
                .viewMode("vote")
                .build();
    }

    public void deleteRoom(String roomId, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        String roomKey = "debate:room:" + roomId;
        Map<Object, Object> roomMap = redisTemplate.opsForHash().entries(roomKey);

        if (roomMap == null || roomMap.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }

        // 권한 확인 (생성한 선생님만 삭제 가능)
        UUID currentUserId = AuthUtils.getUserId(auth);
        String teacherIdStr = (String) roomMap.get("teacherId");

        if (teacherIdStr == null || !teacherIdStr.equals(currentUserId.toString())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator can delete this room");
        }

        String teacherCode = (String) roomMap.get("teacherCode");

        // Redis 데이터 삭제
        redisTemplate.delete(roomKey); // 방정보
        redisTemplate.delete("debate:room:" + roomId + ":messages"); // 메시지
        redisTemplate.delete("debate:room:" + roomId + ":status"); // 찬성/반대 상태

        // 목록에서 제거
        if (teacherCode != null) {
            String teacherCodeIndexKey = "debate:teacherCode:" + teacherCode + ":rooms";
            redisTemplate.opsForZSet().remove(teacherCodeIndexKey, roomId);
        }
    }

    public List<DebateRoomResponseDTO> getRoomsByClassCode(
            Authentication auth, UUID userIdParam) {
        UUID userId;
        if (auth != null && auth.isAuthenticated()) {
            userId = AuthUtils.getUserId(auth);
        } else if (userIdParam != null) {
            userId = userIdParam;
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication or userId required");
        }

        UserEntity user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Integer teacherCode = user.getTeacherCode();

        String classIndexKey = "debate:teacherCode:" + teacherCode + ":rooms";
        Set<String> roomIds = redisTemplate.opsForZSet()
                .reverseRange(classIndexKey, 0, 50);

        if (roomIds == null || roomIds.isEmpty()) {
            return List.of();
        }

        List<DebateRoomResponseDTO> result = new ArrayList<>();

        for (String roomIdStr : roomIds) {
            String roomKey = "debate:room:" + roomIdStr;
            Map<Object, Object> map = redisTemplate.opsForHash().entries(roomKey);
            if (map == null || map.isEmpty())
                continue;

            result.add(DebateRoomResponseDTO.from(map));
        }
        return result;
    }

    public List<ChatMessage> getMessages(String roomId, int page, int size) {
        String key = "debate:room:" + roomId + ":messages";
        int p = Math.max(0, page);
        int s = Math.max(1, Math.min(size, 200));

        Long totalLong = redisTemplate.opsForList().size(key);
        long total = (totalLong == null) ? 0 : totalLong;
        if (total == 0) return List.of();

        long end = total - 1 - ((long) p * s);
        long start = Math.max(0, end - (s - 1));
        if (end < 0) return List.of();

        List<String> jsonList = redisTemplate.opsForList().range(key, start, end);
        if (jsonList == null || jsonList.isEmpty()) return List.of();

        List<ChatMessage> result = new ArrayList<>(jsonList.size());
        for (String json : jsonList) {
            try { result.add(objectMapper.readValue(json, ChatMessage.class)); }
            catch (Exception ignored) {}
        }

        Collections.reverse(result);
        return result;
    }


    public DebateRoomRequestDTO getRoom(String roomId) {
        return activeRooms.get(roomId);
    }

    public void validateJoin(String roomId, UUID userId) {
        String roomKey = "debate:room:" + roomId;

        Map<Object, Object> room = redisTemplate.opsForHash().entries(roomKey);
        if (room == null || room.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }

        String roomTeacherCode = (String) room.get("teacherCode");
        String roomGrade = (String) room.get("grade");
        String roomClassroom = (String) room.get("classroom");
        if (roomTeacherCode == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }

        UserClassInfo userInfo = userRepository.findClassInfoByUserId(userId);
        if (userInfo == null) {
            // Guest user or user not found - Allow for now or handle differently
            return;
        }

        if (userInfo.getTeacherCode() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No teacher code");
        }

        if (!userInfo.getTeacherCode().toString().equals(roomTeacherCode)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not in this class");
        }

        if (roomGrade != null && userInfo.getGrade() != null && !roomGrade.equals(userInfo.getGrade().toString())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Grade mismatch");
        }

        if (roomClassroom != null && userInfo.getClassroom() != null
                && !roomClassroom.equals(userInfo.getClassroom().toString())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Classroom mismatch");
        }
    }

    public String getNickName(UUID userId) {
        return userRepository.findNickNameByUserId(userId);
    }

    public void saveStatus(String roomId, UUID userId, DebateStatus status) {
        String key = "debate:room:" + roomId + ":status";
        redisTemplate.opsForHash().put(key, userId.toString(), status.name());
    }

    public DebateStatus requireStatusSelected(String roomId, UUID userId, SimpMessageHeaderAccessor headerAccessor) {
        // 세션에 있으면 우선 사용
        Map<String, Object> session = headerAccessor.getSessionAttributes();
        if (session != null && session.get("status") != null) {
            return DebateStatus.valueOf(session.get("status").toString());
        }

        // Redis에서 확인
        String key = "debate:room:" + roomId + ":status";
        Object v = redisTemplate.opsForHash().get(key, userId.toString());
        if (v == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Select PRO/CON first");
        }
        return DebateStatus.valueOf(v.toString());
    }

    public void updateRoomMode(String roomId, String viewMode) {
        String roomKey = "debate:room:" + roomId;
        redisTemplate.opsForHash().put(roomKey, "viewMode", viewMode);
    }

    public String resolveNickname(UUID userId, SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> session = headerAccessor.getSessionAttributes();
        if (session != null && session.get("sender") != null) {
            return session.get("sender").toString();
        }
        String nickname = getNickName(userId);
        return nickname != null ? nickname : "unknown";
    }

    public List<ChatMessage> getMessages(String roomId) {
        String key = "debate:room:" + roomId + ":messages";
        List<String> rawMessages = redisTemplate.opsForList().range(key, 0, -1);

        List<ChatMessage> messages = new ArrayList<>();
        if (rawMessages != null) {
            for (String json : rawMessages) {
                try {
                    messages.add(objectMapper.readValue(json, ChatMessage.class));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
        return messages;
    }

    public void appendMessage(String roomId, ChatMessage msg) {
        String key = "debate:room:" + roomId + ":messages";
        try {
            String json = objectMapper.writeValueAsString(msg);
            redisTemplate.opsForList().rightPush(key, json);
            // redisTemplate.opsForList().trim(key, -200, -1);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize ChatMessage", e);
        }
    }

    public void deleteRoom(Integer teacherCode, String roomId, Authentication auth) {
        validateTeacher(auth);
        
        String messagesKey = "debate:room:" + roomId + ":messages";
        String roomKey = "debate:room:" + roomId;
        String teacherRoomsKey = "debate:teacherCode:" + teacherCode + ":rooms";

        redisTemplate.delete(messagesKey);
        redisTemplate.delete(roomKey);
        redisTemplate.opsForZSet().remove(teacherRoomsKey, roomId);
    }


    /**
     * 토론 주제 추천 받기
     * AWS Bedrock Prompt를 통해 한국 역사 토론 주제 3개를 추천받습니다.
     */
    public DebateTopicsResponse getDebateTopicRecommendations(DebateTopicsRequest request) {
        // 1. PromptRequest 생성
        com.lgcns.haibackend.aiPerson.domain.dto.PromptRequest promptRequest = com.lgcns.haibackend.aiPerson.domain.dto.PromptRequest
                .builder()
                .promptId(debateTopicPromptId)
                .userQuery(request.getUserQuery())
                .build();

        // 2. FastAPI를 통해 스트리밍 응답 받기
        String completeResponse = fastApiClient.chatPromptStream(promptRequest)
                .collectList()
                .map(chunks -> String.join("", chunks))
                .block();

        if (completeResponse == null || completeResponse.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답을 받지 못했습니다.");
        }

        // 🔍 디버깅: 완전한 응답 출력
        System.out.println("=== COMPLETE RESPONSE ===");
        System.out.println(completeResponse);
        System.out.println("=== END RESPONSE ===");

        // 3. JSON 파싱하여 DebateTopicsResponse 변환
        try {
            DebateTopicsResponse response = objectMapper.readValue(completeResponse, DebateTopicsResponse.class);
            return response;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI 응답을 파싱하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
