package com.lgcns.haibackend.bedrock.client;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DebugFastApiTest {
    @Test
    public void testRawResponse() throws Exception {
        String jsonBody = "{"
                + "\"query\": \"고려 거란 전쟁\","
                + "\"kb_id\": \"GRRGIM8M5G\","
                + "\"model_arn\": \"arn:aws:bedrock:ap-northeast-2:125814533785:inference-profile/apac.anthropic.claude-sonnet-4-20250514-v1:0\""
                + "}";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8000/chat/knowledge"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        System.out.println("Sending request to http://localhost:8000/chat/knowledge...");

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    System.out.println("Response Status: " + response.statusCode());
                    System.out.println("Response Headers: " + response.headers());
                    System.out.println("Response Body (Raw):");
                    System.out.println(response.body());
                })
                .join();
    }
}
