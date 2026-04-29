package com.smarttraffic.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.backend.model.SiteSettingsEntity;
import com.smarttraffic.backend.repository.SiteSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * LLM 服务，根据 site_settings 中的配置动态创建对应提供商的客户端
 */
@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);

    private final SiteSettingsRepository siteSettingsRepository;
    private final ObjectMapper objectMapper;

    public LlmService(SiteSettingsRepository siteSettingsRepository, ObjectMapper objectMapper) {
        this.siteSettingsRepository = siteSettingsRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 根据当前配置创建 LLM 客户端
     */
    public LlmClient createClient() {
        SiteSettingsEntity settings = siteSettingsRepository.findById(1L).orElseThrow();
        String provider = settings.getLlmProvider();
        String apiKey = settings.getLlmApiKey();
        String baseUrl = settings.getLlmApiBaseUrl();
        String model = settings.getLlmModelName();

        if (provider == null || provider.isBlank() || apiKey == null || apiKey.isBlank() || model == null || model.isBlank()) {
            throw new IllegalStateException("LLM 未配置，请在节点配置管理中设置 AI 配置");
        }

        return switch (provider.toLowerCase()) {
            case "claude" -> new LlmClient.ClaudeLlmClient(apiKey, model, objectMapper);
            default -> new LlmClient.OpenAiLlmClient(
                    baseUrl != null && !baseUrl.isBlank() ? baseUrl : "https://api.openai.com",
                    apiKey, model, objectMapper
            );
        };
    }

    /**
     * 流式调用 LLM（无 tool calling）
     */
    public void streamChat(List<Map<String, String>> messages, Consumer<String> onChunk) {
        LlmClient client = createClient();
        List<Map<String, Object>> objectMessages = messages.stream()
                .map(m -> (Map<String, Object>) new LinkedHashMap<String, Object>(m))
                .toList();
        client.streamChatWithTools(objectMessages, null, onChunk, null);
    }

    /**
     * 测试当前 LLM 配置是否可用
     */
    public boolean testConnection() {
        try {
            return createClient().testConnection();
        } catch (Exception e) {
            log.warn("LLM 连接测试异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查 LLM 是否已配置
     */
    public boolean isConfigured() {
        SiteSettingsEntity settings = siteSettingsRepository.findById(1L).orElse(null);
        if (settings == null) return false;
        return settings.getLlmProvider() != null && !settings.getLlmProvider().isBlank()
                && settings.getLlmApiKey() != null && !settings.getLlmApiKey().isBlank()
                && settings.getLlmModelName() != null && !settings.getLlmModelName().isBlank();
    }

    /**
     * 获取可用模型列表，仅依赖 provider + apiKey + baseUrl，不需要 model
     */
    public List<String> fetchModels() {
        try {
            SiteSettingsEntity settings = siteSettingsRepository.findById(1L).orElseThrow();
            String provider = settings.getLlmProvider();
            String apiKey = settings.getLlmApiKey();
            String baseUrl = settings.getLlmApiBaseUrl();

            if (provider == null || provider.isBlank() || apiKey == null || apiKey.isBlank()) {
                log.warn("获取模型列表失败：缺少 provider 或 apiKey");
                return List.of();
            }

            // Claude 没有公开的 /v1/models 端点
            if ("claude".equalsIgnoreCase(provider)) {
                return List.of();
            }

            String effectiveBaseUrl = baseUrl != null && !baseUrl.isBlank() ? baseUrl : "https://api.openai.com";
            return new LlmClient.OpenAiLlmClient(effectiveBaseUrl, apiKey, "placeholder", objectMapper).fetchModels();
        } catch (Exception e) {
            log.warn("获取模型列表异常: {}", e.getMessage());
            return List.of();
        }
    }

    // ─── Tool Calling 工具定义 ──────────────────────────────────

    /**
     * 返回 4 个工具的 JSON Schema 定义（OpenAI function calling 格式，Claude 也兼容）
     */
    public List<Map<String, Object>> getToolDefinitions() {
        List<Map<String, Object>> tools = new ArrayList<>();

        // query_traffic
        tools.add(buildTool("query_traffic", "查询指定道路的实时交通数据（车流量、车速、拥堵指数等）",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "road_name", Map.of("type", "string", "description", "道路名称")
                        ),
                        "required", List.of("road_name")
                )));

        // list_cameras
        tools.add(buildTool("list_cameras", "获取所有启用的摄像头列表（含经纬度、道路名称、位置）",
                Map.of("type", "object", "properties", Map.of())));

        // query_history
        tools.add(buildTool("query_history", "查询指定道路的历史交通统计数据",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "road_name", Map.of("type", "string", "description", "道路名称"),
                                "hours", Map.of("type", "integer", "description", "查询最近多少小时的数据，默认24，最大168")
                        ),
                        "required", List.of("road_name")
                )));

        // reverse_geocode
        tools.add(buildTool("reverse_geocode", "根据经纬度查找最近的摄像头和道路",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "latitude", Map.of("type", "number", "description", "纬度"),
                                "longitude", Map.of("type", "number", "description", "经度")
                        ),
                        "required", List.of("latitude", "longitude")
                )));

        return tools;
    }

    private Map<String, Object> buildTool(String name, String description, Map<String, Object> parameters) {
        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("type", "function");
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", name);
        function.put("description", description);
        function.put("parameters", parameters);
        tool.put("function", function);
        return tool;
    }

    private static final String DEFAULT_TITLE_PROMPT =
            "根据以下对话内容生成一个简短的中文标题（不超过20字），只返回标题本身，不要加引号或其他格式。";

    /**
     * 使用 LLM 生成对话标题，失败时返回 null（由调用方 fallback）
     */
    public String generateTitle(String userMessage, String assistantReply) {
        try {
            SiteSettingsEntity settings = siteSettingsRepository.findById(1L).orElse(null);
            if (settings == null) return null;

            String provider = settings.getLlmProvider();
            String apiKey = settings.getLlmApiKey();
            String baseUrl = settings.getLlmApiBaseUrl();

            // Use title-specific model if configured, otherwise fall back to main model
            String model = settings.getLlmTitleModelName();
            if (model == null || model.isBlank()) {
                model = settings.getLlmModelName();
            }

            if (provider == null || provider.isBlank() || apiKey == null || apiKey.isBlank()
                    || model == null || model.isBlank()) {
                return null;
            }

            String prompt = settings.getLlmTitlePrompt();
            if (prompt == null || prompt.isBlank()) {
                prompt = DEFAULT_TITLE_PROMPT;
            }

            // Truncate inputs to avoid excessive token usage
            String truncatedUser = userMessage.length() > 200 ? userMessage.substring(0, 200) : userMessage;
            String truncatedReply = assistantReply.length() > 200 ? assistantReply.substring(0, 200) : assistantReply;

            List<Map<String, Object>> titleMessages = List.of(
                    Map.of("role", "system", "content", (Object) prompt),
                    Map.of("role", "user", "content", (Object) ("用户: " + truncatedUser)),
                    Map.of("role", "assistant", "content", (Object) ("助手: " + truncatedReply)),
                    Map.of("role", "user", "content", (Object) "请生成标题")
            );

            LlmClient client = switch (provider.toLowerCase()) {
                case "claude" -> new LlmClient.ClaudeLlmClient(apiKey, model, objectMapper);
                default -> new LlmClient.OpenAiLlmClient(
                        baseUrl != null && !baseUrl.isBlank() ? baseUrl : "https://api.openai.com",
                        apiKey, model, objectMapper
                );
            };

            StringBuilder sb = new StringBuilder();
            client.streamChatWithTools(titleMessages, null, sb::append, null);
            String title = sb.toString().trim();
            // Clean up quotes if the model wraps the title
            if (title.startsWith("\"") && title.endsWith("\"")) {
                title = title.substring(1, title.length() - 1);
            }
            if (title.startsWith("「") && title.endsWith("」")) {
                title = title.substring(1, title.length() - 1);
            }
            return title.isBlank() ? null : title;
        } catch (Exception e) {
            log.warn("标题生成失败: {}", e.getMessage());
            return null;
        }
    }
}
