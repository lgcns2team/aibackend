package com.lgcns.haibackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (JWT 사용 시 불필요)
            .csrf(csrf -> csrf.disable())
            
            // 세션 사용 안 함 (Stateless)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 모든 요청 허용 (JwtFilter에서 직접 검증)
            .authorizeHttpRequests(auth -> 
                auth.anyRequest().permitAll())
            
            // 기본 로그인 폼 비활성화
            .formLogin(form -> form.disable())
            
            // HTTP Basic 인증 비활성화
            .httpBasic(basic -> basic.disable());
        
        return http.build();
    }
}