package com.lgcns.haibackend.discussion;

import com.lgcns.haibackend.discussion.dto.ChatMessage;

import com.lgcns.haibackend.discussion.dto.Room;
import com.lgcns.haibackend.user.domain.entity.UserEntity;
import com.lgcns.haibackend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketRoomIntegrationTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    private UserRepository userRepository;

    private WebSocketStompClient stompClient;

    private UserEntity teacher;
    private UserEntity student;

    @BeforeEach
    void setup() {
        this.stompClient = new WebSocketStompClient(new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        this.stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        // Setup Data
        teacher = UserEntity.builder()
                .name("Teacher Kim")
                .role("TEACHER")
                .grade(1)
                .classroom(1)
                .nickname("T_Kim")
                .password("password")
                .build();
        teacher = userRepository.save(teacher);

        student = UserEntity.builder()
                .name("Student Lee")
                .role("STUDENT")
                .grade(1)
                .classroom(1)
                .nickname("S_Lee")
                .password("password")
                .build();
        student = userRepository.save(student);
    }

    @Test
    void testRoomCreationAndJoin() throws Exception {
        // 1. Create Room via REST API
        RestTemplate restTemplate = new RestTemplate();
        String createRoomUrl = "http://localhost:" + port + "/chat/room";
        Room room = restTemplate.postForObject(createRoomUrl, Map.of("teacherId", teacher.getUserId().toString()),
                Room.class);

        assertThat(room).isNotNull();
        assertThat(room.getRoomId()).isNotNull();

        // 2. Connect via WebSocket
        String wsUrl = String.format("ws://localhost:%d/ws-stomp", port);
        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {
        }).get(1, TimeUnit.SECONDS);

        BlockingQueue<ChatMessage> blockingQueue = new LinkedBlockingDeque<>();

        // 3. Subscribe to Room Topic
        session.subscribe("/topic/room/" + room.getRoomId(), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                blockingQueue.add((ChatMessage) payload);
            }
        });

        // 4. Send Join Message
        ChatMessage joinMessage = ChatMessage.builder()
                .type(ChatMessage.MessageType.JOIN)
                .sender(student.getName())
                .userId(student.getUserId())
                .content("Joining Room")
                .build();

        session.send("/app/chat.addUser/" + room.getRoomId(), joinMessage);

        // 5. Verify Message Received
        ChatMessage receivedMessage = blockingQueue.poll(2, TimeUnit.SECONDS);
        assertThat(receivedMessage).isNotNull();
        assertThat(receivedMessage.getType()).isEqualTo(ChatMessage.MessageType.JOIN);
        assertThat(receivedMessage.getSender()).isEqualTo(student.getName());
    }
}
