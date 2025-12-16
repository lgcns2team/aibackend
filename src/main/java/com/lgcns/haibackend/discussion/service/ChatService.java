package com.lgcns.haibackend.discussion.service;

import com.lgcns.haibackend.discussion.domain.dto.Room;
import com.lgcns.haibackend.user.domain.entity.UserEntity;
import com.lgcns.haibackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final UserRepository userRepository;

    // In-memory storage for active rooms. Key: roomId, Value: Room info
    // In production, use Redis or DB.
    private final Map<String, Room> activeRooms = new ConcurrentHashMap<>();

    public Room createRoom(UUID teacherId) {
        UserEntity teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!"TEACHER".equals(teacher.getRole())) {
            throw new IllegalArgumentException("Only teachers can create rooms");
        }

        String roomId = UUID.randomUUID().toString();
        Room room = Room.builder()
                .roomId(roomId)
                .teacherId(teacherId)
                .grade(teacher.getGrade())
                .classNumber(teacher.getClassroom())
                .build();

        activeRooms.put(roomId, room);
        return room;
    }

    public Room getRoom(String roomId) {
        return activeRooms.get(roomId);
    }

    public void validateJoin(String roomId, UUID studentId) {
        Room room = activeRooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }

        UserEntity student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // If it's the teacher who owns the room, allow
        if (student.getUserId().equals(room.getTeacherId())) {
            return;
        }

        if (!"STUDENT".equals(student.getRole())) {
            // Depending on requirements, maybe other teachers can't join?
            // For now assume only students and the owner teacher.
            throw new IllegalArgumentException("Only students can join rooms");
        }

        if (!student.getGrade().equals(room.getGrade()) || !student.getClassroom().equals(room.getClassNumber())) {
            throw new IllegalArgumentException("Student does not belong to this teacher's class");
        }
    }
}
