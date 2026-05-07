package com.smarttraffic.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * LLM 客户端抽象，统一处理 OpenAI 和 Claude 两种 API 格式
 */
public interface LlmClient {

    /**
     * Tool call 结果
     */
    record ToolCall(String id, String name, String arguments) {}

    /**
     * 流式调用 LLM，支持 tool calling
     * @param messages 消息列表
     * @param tools 工具定义列表（可为 null）
     * @param onChunk 文本 chunk 回调
     * @param onToolCall tool call 回调（可为 null）
     * @return 如果有 tool call 则返回列表，否则返回空列表
     */
    List<ToolCall> streamChatWithTools(
            List<Map<String, Object>> messages,
            List<Map<String, Object>> tools,
            java.util.function.Consumer<String> onChunk,
            BiConsumer<String, String> onToolCall
    );

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
        public List<ToolCall> streamChatWithTools(
                List<Map<String, Object>> messages,
                List<Map<String, Object>> tools,
                java.util.function.Consumer<String> onChunk,
                BiConsumer<String, String> onToolCall) {
            
            List<ToolCall> toolCalls = new ArrayList<>();
            
            try {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("model", model);
                body.put("messages", messages);
                body.put("stream", true);
                if (tools != null && !tools.isEmpty()) {
                    body.put("tools", tools);
                    body.put("tool_choice", "auto");
                }

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
                                        JsonNode delta = node.at("/choices/0/delta");
                                        if (!delta.isMissingNode()) {
                                            // Handle text content
                                            JsonNode contentNode = delta.get("content");
                                            if (contentNode != null && !contentNode.isNull() && contentNode.isTextual()) {
                                                onChunk.accept(contentNode.asText());
                                            }
                                            // Handle tool calls
                                            JsonNode toolCallsNode = delta.get("tool_calls");
                                            if (toolCallsNode != null && toolCallsNode.isArray()) {
                                                for (JsonNode tc : toolCallsNode) {
                                                    int index = tc.path("index").asInt(0);
                                                    String id = tc.path("id").asText("");
                                                    JsonNode func = tc.path("function");
                                                    String name = func.path("name").asText("");
                                                    String args = func.path("arguments").asText("");

                                                    // 确保 list 容量足够
                                                    while (toolCalls.size() <= index) {
                                                        toolCalls.add(new ToolCall("", "", ""));
                                                    }

                                                    ToolCall existing = toolCalls.get(index);
                                                    String mergedId = !id.isEmpty() ? id : existing.id();
                                                    String mergedName = !name.isEmpty() ? name : existing.name();
                                                    String mergedArgs = existing.arguments() + args;
                                                    toolCalls.set(index, new ToolCall(mergedId, mergedName, mergedArgs));

                                                    if (!name.isEmpty() && onToolCall != null) {
                                                        onToolCall.accept(name, args);
                                                    }
                                                }
                                            }
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
            
            return toolCalls;
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
                return java.util.stream.StreamSupport.stream(data.spliterator(), false)
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
        public List<ToolCall> streamChatWithTools(
                List<Map<String, Object>> messages,
                List<Map<String, Object>> tools,
                java.util.function.Consumer<String> onChunk,
                BiConsumer<String, String> onToolCall) {
            
            List<ToolCall> toolCalls = new ArrayList<>();
            
            try {
                // Claude 需要把 system 消息提取出来
                String systemPrompt = messages.stream()
                        .filter(m -> "system".equals(m.get("role")))
                        .map(m -> String.valueOf(m.get("content")))
                        .findFirst()
                        .orElse(null);
                
                List<Map<String, Object>> nonSystemMessages = messages.stream()
                        .filter(m -> !"system".equals(m.get("role")))
                        .toList();

                Map<String, Object> body = new LinkedHashMap<>();
                body.put("model", model);
                body.put("max_tokens", 4096);
                body.put("stream", true);
                body.put("messages", nonSystemMessages);
                if (systemPrompt != null) {
                    body.put("system", systemPrompt);
                }
                if (tools != null && !tools.isEmpty()) {
                    body.put("tools", tools);
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
                                        
                                        switch (type) {
                                            case "content_block_start":
                                                JsonNode contentBlock = node.path("content_block");
                                                if ("tool_use".equals(contentBlock.path("type").asText(""))) {
                                                    String id = contentBlock.path("id").asText("");
                                                    String name = contentBlock.path("name").asText("");
                                                    toolCalls.add(new ToolCall(id, name, ""));
                                                    if (onToolCall != null) {
                                                        onToolCall.accept(name, "");
                                                    }
                                                }
                                                break;
                                            case "content_block_delta":
                                                // Handle text delta
                                                JsonNode textDelta = node.at("/delta/text");
                                                if (!textDelta.isMissingNode() && !textDelta.isNull()) {
                                                    onChunk.accept(textDelta.asText());
                                                }
                                                // Handle tool use input delta
                                                JsonNode delta = node.path("delta");
                                                if ("input_json_delta".equals(delta.path("type").asText(""))) {
                                                    String partialJson = delta.path("partial_json").asText("");
                                                    if (!toolCalls.isEmpty() && partialJson != null) {
                                                        ToolCall last = toolCalls.get(toolCalls.size() - 1);
                                                        toolCalls.set(toolCalls.size() - 1,
                                                                new ToolCall(last.id(), last.name(), last.arguments() + partialJson));
                                                    }
                                                }
                                                break;
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
            
            return toolCalls;
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