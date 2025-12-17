package com.lgcns.haibackend.discussion.service;

import com.lgcns.haibackend.common.security.AuthUtils;
import com.lgcns.haibackend.discussion.domain.dto.DebateRoomRequestDTO;
import com.lgcns.haibackend.discussion.domain.dto.DebateRoomResponseDTO;
import com.lgcns.haibackend.discussion.domain.dto.DebateTopicsRequest;
import com.lgcns.haibackend.discussion.domain.dto.DebateTopicsResponse;
import com.lgcns.haibackend.user.domain.entity.UserClassInfo;
import com.lgcns.haibackend.user.domain.entity.UserEntity;
import com.lgcns.haibackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
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
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final Map<UUID, DebateRoomRequestDTO> activeRooms = new ConcurrentHashMap<>();

    public DebateRoomResponseDTO createRoom(DebateRoomRequestDTO req, Authentication auth) {

        if (!AuthUtils.hasRole(auth, "TEACHER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "선생님만 토론방을 생성할 수 있습니다.");
        }

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
                        .build();
    }

    public List<DebateRoomResponseDTO> getRoomsByClassCode(
            Authentication auth) {
        UUID userId = AuthUtils.getUserId(auth);
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
