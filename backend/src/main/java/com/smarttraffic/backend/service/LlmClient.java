package com.smarttraffic.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

/**
 * LLM 客户端抽象，统一处理 OpenAI 和 Claude 两种 API 格式
 */
public interface LlmClient {

    /**
     * 流式调用 LLM，将每个 chunk 的文本通过 onChunk 回调输出
     */
    void streamChat(List<Map<String, String>> messages, Consumer<String> onChunk);

    /**
     * 测试 LLM 连接是否正常
     */
    boolean testConnection();

    /**
     * 获取可用模型列表
     */
    default List<String> fetchModels() {
        return List.of();
    }

    /**
     * 非流式调用 LLM，返回完整回复文本
     */
    default String chat(List<Map<String, String>> messages, int maxTokens) {
        StringBuilder sb = new StringBuilder();
        streamChat(messages, sb::append);
        return sb.toString();
    }

    /**
     * OpenAI 兼容格式客户端（覆盖 OpenAI、DeepSeek、本地 Ollama 等）
     */
    class OpenAiLlmClient implements LlmClient {
        private static final Logger log = LoggerFactory.getLogger(OpenAiLlmClient.class);
        private final String baseUrl;
        private final String apiKey;
        private final String model;
        private final RestClient restClient;
        private final ObjectMapper objectMapper;

        public OpenAiLlmClient(String baseUrl, String apiKey, String model, ObjectMapper objectMapper) {
            this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            this.apiKey = apiKey;
            this.model = model;
            this.objectMapper = objectMapper;
            this.restClient = RestClient.builder().build();
        }

        @Override
        public void streamChat(List<Map<String, String>> messages, Consumer<String> onChunk) {
            try {
                Map<String, Object> body = Map.of(
                        "model", model,
                        "messages", messages,
                        "stream", true
                );

                restClient.post()
                        .uri(baseUrl + "/v1/chat/completions")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + apiKey)
                        .body(body)
                        .exchange((request, response) -> {
                            try (var reader = new java.io.BufferedReader(
                                    new java.io.InputStreamReader(response.getBody(), java.nio.charset.StandardCharsets.UTF_8))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    if (line.startsWith("data: ")) {
                                        String data = line.substring(6).trim();
                                        if ("[DONE]".equals(data)) break;
                                        JsonNode node = objectMapper.readTree(data);
                                        JsonNode delta = node.at("/choices/0/delta/content");
                                        if (!delta.isMissingNode() && !delta.isNull()) {
                                            onChunk.accept(delta.asText());
                                        }
                                    }
                                }
                            }
                            return null;
                        });
            } catch (Exception e) {
                log.error("OpenAI 流式调用失败: {}", e.getMessage());
                onChunk.accept("[错误] AI 服务调用失败: " + e.getMessage());
            }
        }

        @Override
        public boolean testConnection() {
            try {
                Map<String, Object> body = Map.of(
                        "model", model,
                        "messages", List.of(Map.of("role", "user", "content", "Say hi")),
                        "max_tokens", 10
                );
                restClient.post()
                        .uri(baseUrl + "/v1/chat/completions")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + apiKey)
                        .body(body)
                        .retrieve()
                        .body(String.class);
                return true;
            } catch (Exception e) {
                log.warn("OpenAI 连接测试失败: {}", e.getMessage());
                return false;
            }
        }

        @Override
        public List<String> fetchModels() {
            try {
                String response = restClient.get()
                        .uri(baseUrl + "/v1/models")
                        .header("Authorization", "Bearer " + apiKey)
                        .retrieve()
                        .body(String.class);
                if (response == null) return List.of();
                JsonNode root = objectMapper.readTree(response);
                JsonNode data = root.path("data");
                if (!data.isArray()) return List.of();
                return StreamSupport.stream(data.spliterator(), false)
                        .map(node -> node.path("id").asText(""))
                        .filter(id -> !id.isEmpty())
                        .sorted()
                        .toList();
            } catch (Exception e) {
                log.warn("获取模型列表失败: {}", e.getMessage());
                return List.of();
            }
        }
    }

    /**
     * Claude API 客户端（Anthropic 原生格式）
     */
    class ClaudeLlmClient implements LlmClient {
        private static final Logger log = LoggerFactory.getLogger(ClaudeLlmClient.class);
        private final String apiKey;
        private final String model;
        private final RestClient restClient;
        private final ObjectMapper objectMapper;

        public ClaudeLlmClient(String apiKey, String model, ObjectMapper objectMapper) {
            this.apiKey = apiKey;
            this.model = model;
            this.objectMapper = objectMapper;
            this.restClient = RestClient.builder().build();
        }

        @Override
        public void streamChat(List<Map<String, String>> messages, Consumer<String> onChunk) {
            try {
                // Claude 需要把 system 消息提取出来
                String systemPrompt = messages.stream()
                        .filter(m -> "system".equals(m.get("role")))
                        .map(m -> m.get("content"))
                        .findFirst()
                        .orElse(null);
                List<Map<String, String>> nonSystemMessages = messages.stream()
                        .filter(m -> !"system".equals(m.get("role")))
                        .toList();

                Map<String, Object> body = new java.util.LinkedHashMap<>();
                body.put("model", model);
                body.put("max_tokens", 4096);
                body.put("stream", true);
                body.put("messages", nonSystemMessages);
                if (systemPrompt != null) {
                    body.put("system", systemPrompt);
                }

                restClient.post()
                        .uri("https://api.anthropic.com/v1/messages")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header("x-api-key", apiKey)
                        .header("anthropic-version", "2023-06-01")
                        .body(body)
                        .exchange((request, response) -> {
                            try (var reader = new java.io.BufferedReader(
                                    new java.io.InputStreamReader(response.getBody(), java.nio.charset.StandardCharsets.UTF_8))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    if (line.startsWith("data: ")) {
                                        String data = line.substring(6).trim();
                                        JsonNode node = objectMapper.readTree(data);
                                        String type = node.path("type").asText("");
                                        if ("content_block_delta".equals(type)) {
                                            JsonNode textNode = node.at("/delta/text");
                                            if (!textNode.isMissingNode() && !textNode.isNull()) {
                                                onChunk.accept(textNode.asText());
                                            }
                                        }
                                    }
                                }
                            }
                            return null;
                        });
            } catch (Exception e) {
                log.error("Claude 流式调用失败: {}", e.getMessage());
                onChunk.accept("[错误] AI 服务调用失败: " + e.getMessage());
            }
        }

        @Override
        public boolean testConnection() {
            try {
                Map<String, Object> body = Map.of(
                        "model", model,
                        "max_tokens", 5,
                        "messages", List.of(Map.of("role", "user", "content", "Say hi"))
                );
                restClient.post()
                        .uri("https://api.anthropic.com/v1/messages")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header("x-api-key", apiKey)
                        .header("anthropic-version", "2023-06-01")
                        .body(body)
                        .retrieve()
                        .body(String.class);
                return true;
            } catch (Exception e) {
                log.warn("Claude 连接测试失败: {}", e.getMessage());
                return false;
            }
        }
    }
}
