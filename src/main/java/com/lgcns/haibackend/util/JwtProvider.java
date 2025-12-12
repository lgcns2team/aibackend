package com.lgcns.haibackend.util;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secret;

    private Key getStringKey() {

        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getStringKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    // [추가] role을 추출하는 메서드입니다.
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getStringKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("role", String.class);
    }

    // 토큰 유효성 검사 메서드 (RefreshToken 재발급 로직에 필요)
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getStringKey()).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            // 토큰 만료, 위변조 등 예외 처리 (여기서는 단순히 false 반환)
            System.err.println("JWT Token Validation Error: " + e.getMessage());
            return false;
        }
    }
}
