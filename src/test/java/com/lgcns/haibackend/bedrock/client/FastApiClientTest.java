package com.lgcns.haibackend.bedrock.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.mockito.Mockito.mock;

public class FastApiClientTest {

    @Test
    public void testMultibyteCharacterSplitting() {
        // Given
        FastApiClient client = new FastApiClient(mock(org.springframework.web.reactive.function.client.WebClient.class),
                new ObjectMapper());

        // "가" in UTF-8 is 3 bytes: EA B0 80
        // We will split it across two buffers: [EA B0] and [80]
        byte[] part1 = new byte[] { (byte) 0xEA, (byte) 0xB0 };
        byte[] part2 = new byte[] { (byte) 0x80 };

        String json1 = "{\"type\":\"content\",\"text\":\"";
        String json2 = "\"}";

        // Construct SSE data: data: {"type":"content","text":"가"}\n\n
        // But split across buffers

        DefaultDataBufferFactory factory = new DefaultDataBufferFactory();

        // Buffer 1: "data: {"type":"content","text":"" + part1 of "가"
        DataBuffer buffer1 = factory.allocateBuffer();
        buffer1.write("data: ".getBytes(StandardCharsets.UTF_8));
        buffer1.write(json1.getBytes(StandardCharsets.UTF_8));
        buffer1.write(part1);

        // Buffer 2: part2 of "가" + "}" + "\n\n"
        DataBuffer buffer2 = factory.allocateBuffer();
        buffer2.write(part2);
        buffer2.write(json2.getBytes(StandardCharsets.UTF_8));
        buffer2.write("\n\n".getBytes(StandardCharsets.UTF_8));

        Flux<DataBuffer> body = Flux.just(buffer1, buffer2);

        // When
        // We need to access the private method or refactor it to be
        // package-private/protected for testing.
        // For now, let's use reflection or just assume we can call a public method that
        // uses it.
        // Since `decodeAndParseSse` is private, we can't call it directly easily
        // without reflection.
        // Let's use reflection to invoke the private method.

        Flux<String> result = invokeDecodeAndParseSse(client, body);

        // Then
        StepVerifier.create(result)
                .expectNext("가")
                .verifyComplete();
    }

    private Flux<String> invokeDecodeAndParseSse(FastApiClient client, Flux<DataBuffer> body) {
        try {
            java.lang.reflect.Method method = FastApiClient.class.getDeclaredMethod("decodeAndParseSse", Flux.class);
            method.setAccessible(true);
            return (Flux<String>) method.invoke(client, body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
