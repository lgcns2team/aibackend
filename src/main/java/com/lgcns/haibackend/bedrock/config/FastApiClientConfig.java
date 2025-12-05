package com.lgcns.haibackend.bedrock.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * FastAPI 클라이언트 설정
 */
@Configuration
public class FastApiClientConfig {

    @Value("${fastapi.base-url:http://localhost:8000}")
    private String baseUrl;

    @Value("${fastapi.timeout.connect:5000}")
    private int connectTimeout;

    @Value("${fastapi.timeout.read:60000}")
    private int readTimeout;

    @Value("${fastapi.timeout.write:5000}")
    private int writeTimeout;

    /**
     * WebClient 빈 (비동기/스트리밍 용)
     * 권장 방식: 비동기 처리 및 스트리밍에 최적화
     */
    @Bean
    public WebClient webClient(ObjectMapper objectMapper) {
        // Connection Pool 설정
        ConnectionProvider connectionProvider = ConnectionProvider.builder("fastapi-pool")
                .maxConnections(100)
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofSeconds(60))
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .evictInBackground(Duration.ofSeconds(120))
                .build();

        // HttpClient 설정
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .responseTimeout(Duration.ofMillis(readTimeout))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS))
                );

        // 큰 응답을 처리하기 위한 메모리 설정
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024); // 16MB
                    configurer.defaultCodecs().jackson2JsonEncoder(
                            new Jackson2JsonEncoder(objectMapper)
                    );
                    configurer.defaultCodecs().jackson2JsonDecoder(
                            new Jackson2JsonDecoder(objectMapper)
                    );
                })
                .build();

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .build();
    }

    /**
     * RestTemplate 빈 (동기 방식)
     * 간단한 요청에 적합
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // 타임아웃 설정을 위한 RequestFactory 커스터마이징
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = 
            new org.springframework.http.client.SimpleClientHttpRequestFactory();
        
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        
        restTemplate.setRequestFactory(factory);
        
        return restTemplate;
    }

    /**
     * ObjectMapper 빈
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules();
    }
}