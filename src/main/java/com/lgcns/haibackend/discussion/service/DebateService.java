package com.lgcns.haibackend.discussion.service;

import com.lgcns.haibackend.common.security.AuthUtils;
import com.lgcns.haibackend.discussion.domain.dto.DebateRoomRequestDTO;
import com.lgcns.haibackend.discussion.domain.dto.DebateRoomResponseDTO;
import com.lgcns.haibackend.user.domain.entity.UserEntity;
import com.lgcns.haibackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class DebateService {

    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final Map<UUID, DebateRoomRequestDTO> activeRooms = new ConcurrentHashMap<>();

    public DebateRoomResponseDTO createRoom(DebateRoomRequestDTO req, Authentication auth) {

        if (!AuthUtils.hasRole(auth, "TEACHER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "선생님만 토론방을 생성할 수 있습니다.");
        }

        UUID teacherId = AuthUtils.getUserId(auth);

        UserEntity teacher = userRepository.findByUserId(teacherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Integer tCode = teacher.getTCode();
        UUID roomId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        String roomKey = "debate:room:" + roomId;
        Map<String, String> roomMap = new HashMap<>();
        roomMap.put("roomId", roomId.toString());
        roomMap.put("teacherId", teacherId.toString());
        roomMap.put("tCode", tCode.toString());
        roomMap.put("grade", req.getGrade().toString());
        roomMap.put("classroom", req.getClassroom().toString());
        roomMap.put("topicTitle", req.getTopicTitle());
        roomMap.put("topicDescription", req.getTopicDescription());
        roomMap.put("participantCount", req.getParticipantCount().toString());
        roomMap.put("createdAt", createdAt.toString());

        redisTemplate.opsForHash().putAll(roomKey, roomMap);

        // 클래스코드별 토론방
        String classCodeIndexKey = "debate:classCode:" + tCode + ":rooms";
        redisTemplate.opsForZSet().add(
            classCodeIndexKey,
            roomId.toString(),
            createdAt.atZone(ZoneId.systemDefault()).toEpochSecond()
        );

        return DebateRoomResponseDTO.builder()
                        .roomId(roomId)
                        .teacherId(teacherId)
                        .tCode(tCode)
                        .participantCount(req.getParticipantCount())
                        .topicTitle(req.getTopicTitle())
                        .topicDescription(req.getTopicDescription())
                        .createdAt(createdAt)
                        .build();
    }

    public List<DebateRoomResponseDTO> getRoomsByClassCode(
            Authentication auth
    ) {
        UUID userId = AuthUtils.getUserId(auth);
        UserEntity user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Integer tCode = user.getTCode();

        if (tCode == null) {
            return List.of();
        }

        String classIndexKey = "debate:classCode:" + tCode + ":rooms";
        Set<String> roomIds = redisTemplate.opsForZSet()
                .reverseRange(classIndexKey, 0, 50);

        if (roomIds == null || roomIds.isEmpty()) {
            return List.of();
        }

        List<DebateRoomResponseDTO> result = new ArrayList<>();

        for (String roomIdStr : roomIds) {
            String roomKey = "debate:room:" + roomIdStr;
            Map<Object, Object> map = redisTemplate.opsForHash().entries(roomKey);
            if (map == null || map.isEmpty()) continue;

            result.add(DebateRoomResponseDTO.from(map));
        }
        return result;
    }

    public DebateRoomRequestDTO getRoom(String roomId) {
        return activeRooms.get(roomId);
    }

    public void validateJoin(String roomId, UUID userId) {
       String roomKey = "debate:room:" + roomId;
        String roomClassCode = (String) redisTemplate.opsForHash().get(roomKey, "classCode");
        if (roomClassCode == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }

        UUID userClassCode = userRepository.findClassCodeByUserId(userId);
        if (userClassCode == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No class code");
        }

        if (!userClassCode.toString().equals(roomClassCode)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not in this class");
        }
    }

    public String getNickname(UUID userId) {
        return userRepository.findNickNameByUserId(userId);
    }
}
