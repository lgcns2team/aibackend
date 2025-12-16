package com.lgcns.haibackend.discussion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import com.lgcns.haibackend.discussion.domain.dto.ChatMessage;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketChatIntegrationTest {

    @LocalServerPort
    private Integer port;

    private WebSocketStompClient stompClient;

    @BeforeEach
    void setup() {
        this.stompClient = new WebSocketStompClient(new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        this.stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @Test
    void verifySendMessage() throws Exception {
        String url = String.format("ws://localhost:%d/ws-stomp", port);
        StompSession session = stompClient.connectAsync(url, new StompSessionHandlerAdapter() {
        }).get(1, TimeUnit.SECONDS);

        BlockingQueue<ChatMessage> blockingQueue = new LinkedBlockingDeque<>();

        session.subscribe("/topic/public", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                blockingQueue.add((ChatMessage) payload);
            }
        });

        ChatMessage message = ChatMessage.builder()
                .type(ChatMessage.MessageType.CHAT)
                .sender("testUser")
                .content("Hello, WebSocket!")
                .build();

        session.send("/app/chat.sendMessage", message);

        ChatMessage receivedMessage = blockingQueue.poll(1, TimeUnit.SECONDS);

        assertThat(receivedMessage).isNotNull();
        assertThat(receivedMessage.getContent()).isEqualTo("Hello, WebSocket!");
        assertThat(receivedMessage.getSender()).isEqualTo("testUser");
    }
}
