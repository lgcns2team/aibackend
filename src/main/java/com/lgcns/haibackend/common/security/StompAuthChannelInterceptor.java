package com.lgcns.haibackend.common.security;

import java.util.Collections;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.lgcns.haibackend.util.JwtProvider;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            log.debug("📭 Accessor is null");
            return message;
        }

        log.info("STOMP Message: Command={}, SessionId={}", 
            accessor.getCommand(), 
            accessor.getSessionId());

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.info("Processing CONNECT command");
            
            String token = null;

            // 1. Authorization 헤더에서 추출
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null) {
                authHeader = accessor.getFirstNativeHeader("authorization");
            }
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7).trim();
                log.info("Token from Authorization header");
            }

            // 2. 세션 속성에서 추출 (HandshakeInterceptor에서 저장한 것)
            if (token == null && accessor.getSessionAttributes() != null) {
                Object tokenObj = accessor.getSessionAttributes().get("token");
                if (tokenObj != null) {
                    token = tokenObj.toString();
                    log.info("Token from session attributes (handshake)");
                }
            }

            if (token == null) {
                log.error("STOMP CONNECT: No token found!");
                log.debug("Session attributes: {}", accessor.getSessionAttributes());
                log.debug("Native headers: {}", accessor.toNativeHeaderMap());
                return message;
            }

            try {
                String userId = jwtProvider.getUserIdFromToken(token);
                String role = jwtProvider.getRoleFromToken(token);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                accessor.setUser(authentication);
                
                // 세션에 인증 정보 저장
                if (accessor.getSessionAttributes() != null) {
                    accessor.getSessionAttributes().put("STOMP_AUTH", authentication);
                    log.info("STOMP CONNECT SUCCESS: userId={}, role={}, sessionId={}", 
                        userId, role, accessor.getSessionId());
                } else {
                    log.warn("Session attributes is null!");
                }

            } catch (Exception e) {
                log.error("STOMP CONNECT: Token validation failed: {}", e.getMessage(), e);
            }
        }
        // 다른 명령어에서 세션에서 인증 정보 복원
        else {
            if (accessor.getUser() == null) {
                log.debug("User is null for command: {}, trying to restore from session", accessor.getCommand());
                
                if (accessor.getSessionAttributes() != null) {
                    UsernamePasswordAuthenticationToken auth = 
                        (UsernamePasswordAuthenticationToken) accessor.getSessionAttributes().get("STOMP_AUTH");
                    
                    if (auth != null) {
                        accessor.setUser(auth);
                        log.info("Restored auth from session: userId={}, command={}", 
                            auth.getPrincipal(), accessor.getCommand());
                    } else {
                        log.warn("No STOMP_AUTH in session for command: {} (sessionId={})", 
                            accessor.getCommand(), accessor.getSessionId());
                        log.debug("Session contents: {}", accessor.getSessionAttributes().keySet());
                    }
                } else {
                    log.warn("Session attributes is null for command: {}", accessor.getCommand());
                }
            } else {
                log.debug("User already present: {} for command: {}", 
                    accessor.getUser().getName(), accessor.getCommand());
            }
        }

        return message;
    }
}
