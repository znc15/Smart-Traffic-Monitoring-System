package com.smarttraffic.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.backend.model.SiteSettingsEntity;
import com.smarttraffic.backend.repository.SiteSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
     * 流式调用 LLM
     */
    public void streamChat(List<Map<String, String>> messages, Consumer<String> onChunk) {
        LlmClient client = createClient();
        client.streamChat(messages, onChunk);
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

            List<Map<String, String>> messages = List.of(
                    Map.of("role", "system", "content", prompt),
                    Map.of("role", "user", "content", "用户: " + truncatedUser),
                    Map.of("role", "assistant", "content", "助手: " + truncatedReply),
                    Map.of("role", "user", "content", "请生成标题")
            );

            LlmClient client = switch (provider.toLowerCase()) {
                case "claude" -> new LlmClient.ClaudeLlmClient(apiKey, model, objectMapper);
                default -> new LlmClient.OpenAiLlmClient(
                        baseUrl != null && !baseUrl.isBlank() ? baseUrl : "https://api.openai.com",
                        apiKey, model, objectMapper
                );
            };

            String title = client.chat(messages, 50).trim();
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
