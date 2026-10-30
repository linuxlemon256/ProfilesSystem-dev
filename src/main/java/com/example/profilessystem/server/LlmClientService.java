package com.example.profilessystem.server;

import tools.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Service
public class LlmClientService {
    private final RestClient.Builder restClientBuilder;
    private final LlmQuotaService settings;

    public LlmClientService(RestClient.Builder restClientBuilder, LlmQuotaService settings) {
        this.restClientBuilder = restClientBuilder;
        this.settings = settings;
    }

    public ChatResult chat(String message) {
        if (!settings.modelConfigured()) {
            throw new IllegalStateException("模型服务尚未完成配置");
        }
        return settings.provider().startsWith("ollama") ? chatWithOllama(message) : chatWithOpenApi(message);
    }

    private ChatResult chatWithOpenApi(String message) {
        String endpoint = appendPath(settings.baseUrl(), "/chat/completions");
        Map<String, Object> body = Map.of(
                "model", settings.modelName(),
                "messages", List.of(Map.of("role", "user", "content", message)),
                "stream", false
        );
        try {
            JsonNode response = restClientBuilder.build().post().uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + settings.apiKey())
                    .body(body).retrieve().body(JsonNode.class);
            String reply = response == null ? "" : response.path("choices").path(0).path("message").path("content").asText("");
            int tokens = response == null ? 0 : response.path("usage").path("total_tokens").asInt(0);
            return requireReply(reply, tokens);
        } catch (RestClientException exception) {
            throw new IllegalStateException("OpenAPI 模型请求失败，请检查服务地址、模型名称和 API Key", exception);
        }
    }

    private ChatResult chatWithOllama(String message) {
        String endpoint = appendPath(settings.baseUrl(), "/api/chat");
        Map<String, Object> body = Map.of(
                "model", settings.modelName(),
                "messages", List.of(Map.of("role", "user", "content", message)),
                "stream", false
        );
        try {
            JsonNode response = restClientBuilder.build().post().uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(JsonNode.class);
            String reply = response == null ? "" : response.path("message").path("content").asText("");
            int tokens = response == null ? 0 : response.path("prompt_eval_count").asInt(0) + response.path("eval_count").asInt(0);
            return requireReply(reply, tokens);
        } catch (RestClientException exception) {
            throw new IllegalStateException("Ollama 请求失败，请确认服务地址可访问、服务已启动且模型已经下载", exception);
        }
    }

    private ChatResult requireReply(String reply, int tokens) {
        if (reply == null || reply.isBlank()) {
            throw new IllegalStateException("模型服务没有返回有效内容");
        }
        return new ChatResult(reply, Math.max(0, tokens));
    }

    private String appendPath(String baseUrl, String path) {
        String normalized = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        if (normalized.endsWith(path)) return normalized;
        return normalized + path;
    }

    public record ChatResult(String reply, int totalTokens) {}
}
