package com.smarttraffic.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.model.AiChatConversationEntity;
import com.smarttraffic.backend.model.AiChatMessageEntity;
import com.smarttraffic.backend.repository.AiChatConversationRepository;
import com.smarttraffic.backend.repository.AiChatMessageRepository;
import com.smarttraffic.backend.security.CurrentUser;
import com.smarttraffic.backend.security.SecurityUtils;
import com.smarttraffic.backend.service.AiToolExecutor;
import com.smarttraffic.backend.service.LlmClient;
import com.smarttraffic.backend.service.LlmService;
import com.smarttraffic.backend.service.TrafficService;
import com.smarttraffic.backend.service.analytics.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/v1/ai")
public class AiAssistantController {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantController.class);
    private static final int MAX_TOOL_ITERATIONS = 5;

    private final LlmService llmService;
    private final AiToolExecutor toolExecutor;
    private final AiChatConversationRepository conversationRepo;
    private final AiChatMessageRepository messageRepo;
    private final TrafficService trafficService;
    private final RedisCacheService redisCacheService;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public AiAssistantController(LlmService llmService,
                                 AiToolExecutor toolExecutor,
                                 AiChatConversationRepository conversationRepo,
                                 AiChatMessageRepository messageRepo,
                                 TrafficService trafficService,
                                 RedisCacheService redisCacheService,
                                 ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.toolExecutor = toolExecutor;
        this.conversationRepo = conversationRepo;
        this.messageRepo = messageRepo;
        this.trafficService = trafficService;
        this.redisCacheService = redisCacheService;
        this.objectMapper = objectMapper;
    }

    // ─── 对话列表（支持分页）──────────────────────────────────

    @GetMapping("/conversations")
    public Map<String, Object> listConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));

        if (page == 0) {
            var cached = redisCacheService.getAiConversations(user.id(), Map.class);
            if (cached.isPresent()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) cached.get();
                return result;
            }
        }

        Page<AiChatConversationEntity> pageResult =
                conversationRepo.findByUserIdOrderByUpdatedAtDesc(user.id(), pageable);

        List<Map<String, Object>> items = new ArrayList<>();
        for (var c : pageResult.getContent()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("title", c.getTitle());
            item.put("road_context", c.getRoadContext());
            item.put("created_at", c.getCreatedAt());
            item.put("updated_at", c.getUpdatedAt());
            items.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", pageResult.getTotalElements());
        result.put("page", page);
        result.put("size", size);

        if (page == 0) {
            redisCacheService.putAiConversations(user.id(), result);
        }
        return result;
    }

    // ─── 创建对话 ──────────────────────────────────────────────

    @PostMapping("/conversations")
    public Map<String, Object> createConversation(@RequestBody Map<String, String> body) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        AiChatConversationEntity conv = new AiChatConversationEntity();
        conv.setUserId(user.id());
        conv.setTitle(body.getOrDefault("title", "新对话"));
        conv.setRoadContext(body.get("road_context"));
        conversationRepo.save(conv);
        redisCacheService.evictAiConversations(user.id());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", conv.getId());
        result.put("title", conv.getTitle());
        result.put("road_context", conv.getRoadContext());
        result.put("created_at", conv.getCreatedAt());
        result.put("updated_at", conv.getUpdatedAt());
        return result;
    }

    // ─── 更新对话标题 ──────────────────────────────────────────

    @PatchMapping("/conversations/{id}")
    public Map<String, Object> updateConversation(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        AiChatConversationEntity conv = getConversationForUser(id, user.id());

        String title = body.get("title");
        if (title != null && !title.isBlank()) {
            conv.setTitle(title);
        }
        String roadContext = body.get("road_context");
        if (roadContext != null) {
            conv.setRoadContext(roadContext);
        }
        conversationRepo.save(conv);
        redisCacheService.evictAiConversations(user.id());
        redisCacheService.evictAiMessages(id);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", conv.getId());
        result.put("title", conv.getTitle());
        result.put("road_context", conv.getRoadContext());
        result.put("created_at", conv.getCreatedAt());
        result.put("updated_at", conv.getUpdatedAt());
        return result;
    }

    // ─── 删除对话 ──────────────────────────────────────────────

    @Transactional
    @DeleteMapping("/conversations/{id}")
    public Map<String, String> deleteConversation(@PathVariable Long id) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        getConversationForUser(id, user.id());
        messageRepo.deleteByConversationId(id);
        conversationRepo.deleteById(id);
        redisCacheService.evictAiConversations(user.id());
        redisCacheService.evictAiMessages(id);
        return Map.of("message", "删除成功");
    }

    // ─── 清空对话消息 ──────────────────────────────────────────

    @Transactional
    @DeleteMapping("/conversations/{id}/messages")
    public Map<String, String> clearConversationMessages(@PathVariable Long id) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        getConversationForUser(id, user.id());
        messageRepo.deleteByConversationId(id);
        redisCacheService.evictAiMessages(id);
        return Map.of("message", "已清空对话消息");
    }

    // ─── 对话消息历史 ──────────────────────────────────────────

    @GetMapping("/conversations/{id}/messages")
    public List<Map<String, Object>> getMessages(@PathVariable Long id) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        getConversationForUser(id, user.id());

        var cached = redisCacheService.getAiMessages(id, List.class);
        if (cached.isPresent()) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> result = (List<Map<String, Object>>) cached.get();
            return result;
        }

        List<AiChatMessageEntity> messages = messageRepo.findByConversationIdOrderByCreatedAtAsc(id);
        List<Map<String, Object>> result = new ArrayList<>();
        for (var m : messages) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.getId());
            item.put("role", m.getRole());
            item.put("content", m.getContent());
            item.put("created_at", m.getCreatedAt());
            result.add(item);
        }
        redisCacheService.putAiMessages(id, result);
        return result;
    }

    // ─── 流式聊天（SSE）支持 Tool Calling ───────────────────────

    @PostMapping(value = "/conversations/{id}/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@PathVariable Long id, @RequestBody Map<String, String> body) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        AiChatConversationEntity conv = getConversationForUser(id, user.id());

        String userMessage = body.get("content");
        if (userMessage == null || userMessage.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "消息内容不能为空");
        }

        // 保存用户消息
        AiChatMessageEntity userMsg = new AiChatMessageEntity();
        userMsg.setConversationId(id);
        userMsg.setRole("user");
        userMsg.setContent(userMessage);
        messageRepo.save(userMsg);

        // 加载历史消息构建上下文
        List<AiChatMessageEntity> history = messageRepo.findByConversationIdOrderByCreatedAtAsc(id);
        
        // 构建消息列表（使用 Object 类型以支持 tool result）
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", buildSystemPrompt(conv.getRoadContext())));
        for (var m : history) {
            messages.add(Map.of("role", m.getRole(), "content", m.getContent()));
        }

        // 首条消息时设置初始标题
        final boolean needsTitle = history.size() <= 2;
        if (needsTitle) {
            String fallbackTitle = userMessage.length() > 30 ? userMessage.substring(0, 30) + "..." : userMessage;
            conv.setTitle(fallbackTitle);
            conversationRepo.save(conv);
        }

        SseEmitter emitter = new SseEmitter(180_000L); // 3 分钟超时
        StringBuilder fullResponse = new StringBuilder();

        executor.execute(() -> {
            try {
                LlmClient client = llmService.createClient();
                List<Map<String, Object>> tools = llmService.getToolDefinitions();
                
                // 多轮 tool call 循环
                int iteration = 0;
                while (iteration < MAX_TOOL_ITERATIONS) {
                    iteration++;
                    
                    // 调用 LLM
                    List<LlmClient.ToolCall> toolCalls = client.streamChatWithTools(
                            messages,
                            tools,
                            chunk -> {
                                try {
                                    fullResponse.append(chunk);
                                    emitter.send(SseEmitter.event().name("chunk").data(chunk));
                                } catch (Exception e) {
                                    log.warn("SSE 发送 chunk 失败: {}", e.getMessage());
                                }
                            },
                            null
                    );

                    // 如果没有 tool call，结束循环
                    if (toolCalls.isEmpty()) {
                        break;
                    }

                    // 处理每个 tool call
                    for (LlmClient.ToolCall tc : toolCalls) {
                        try {
                            // 发送 tool_call 事件
                            Map<String, Object> toolCallInfo = new LinkedHashMap<>();
                            toolCallInfo.put("name", tc.name());
                            toolCallInfo.put("arguments", tc.arguments());
                            emitter.send(SseEmitter.event()
                                    .name("tool_call")
                                    .data(objectMapper.writeValueAsString(toolCallInfo)));

                            // 执行工具
                            Map<String, Object> toolResult = toolExecutor.execute(tc.name(), tc.arguments());

                            // 发送 tool_result 事件
                            emitter.send(SseEmitter.event()
                                    .name("tool_result")
                                    .data(objectMapper.writeValueAsString(toolResult)));

                            // 将 assistant 消息（tool call）和 tool result 加入消息历史
                            Map<String, Object> assistantMsg = new LinkedHashMap<>();
                            assistantMsg.put("role", "assistant");
                            assistantMsg.put("content", null);
                            Map<String, Object> toolCallData = new LinkedHashMap<>();
                            toolCallData.put("id", tc.id());
                            toolCallData.put("type", "function");
                            Map<String, Object> functionData = new LinkedHashMap<>();
                            functionData.put("name", tc.name());
                            functionData.put("arguments", tc.arguments());
                            toolCallData.put("function", functionData);
                            assistantMsg.put("tool_calls", List.of(toolCallData));
                            messages.add(assistantMsg);

                            // Tool result 消息
                            Map<String, Object> toolResultMsg = new LinkedHashMap<>();
                            toolResultMsg.put("role", "tool");
                            toolResultMsg.put("tool_call_id", tc.id());
                            toolResultMsg.put("content", objectMapper.writeValueAsString(toolResult));
                            messages.add(toolResultMsg);

                        } catch (Exception e) {
                            log.error("Tool 执行失败: {} - {}", tc.name(), e.getMessage());
                            try {
                                Map<String, Object> errorResult = Map.of(
                                        "success", false,
                                        "error", "工具执行失败: " + e.getMessage()
                                );
                                emitter.send(SseEmitter.event()
                                        .name("tool_result")
                                        .data(objectMapper.writeValueAsString(errorResult)));
                            } catch (Exception ignored) {}
                        }
                    }
                }

                // 保存助手回复
                AiChatMessageEntity assistantMsg = new AiChatMessageEntity();
                assistantMsg.setConversationId(id);
                assistantMsg.setRole("assistant");
                assistantMsg.setContent(fullResponse.toString());
                messageRepo.save(assistantMsg);
                redisCacheService.evictAiMessages(id);
                redisCacheService.evictAiConversations(conv.getUserId());

                // 异步生成标题
                if (needsTitle && fullResponse.length() > 0) {
                    String assistantReply = fullResponse.toString();
                    executor.execute(() -> {
                        try {
                            String generated = llmService.generateTitle(userMessage, assistantReply);
                            if (generated != null && !generated.isBlank()) {
                                conv.setTitle(generated);
                                conversationRepo.save(conv);
                                redisCacheService.evictAiConversations(conv.getUserId());
                                log.info("标题生成成功: {}", generated);
                            }
                        } catch (Exception ex) {
                            log.warn("异步标题生成失败: {}", ex.getMessage());
                        }
                    });
                }

                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                log.error("AI 聊天异常: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("AI 服务异常: " + e.getMessage()));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    // ─── 检查 AI 是否可用 ──────────────────────────────────────

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("configured", llmService.isConfigured());
    }

    // ─── 辅助方法 ──────────────────────────────────────────────

    private AiChatConversationEntity getConversationForUser(Long id, Long userId) {
        AiChatConversationEntity conv = conversationRepo.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "对话不存在"));
        if (!conv.getUserId().equals(userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "无权访问此对话");
        }
        return conv;
    }

    // ─── 系统提示词构建 ────────────────────────────────────────

    private String buildSystemPrompt(String roadContext) {
        StringBuilder sb = new StringBuilder();

        sb.append("你是「智能交通监控系统」的 AI 助手。你可以使用工具查询实时交通数据。\n\n");
        
        sb.append("## 可用工具\n");
        sb.append("- `query_traffic`: 查询指定道路的实时交通数据（车流量、车速、拥堵指数等）\n");
        sb.append("- `list_cameras`: 获取所有摄像头列表（含经纬度、道路名称）\n");
        sb.append("- `query_history`: 查询指定道路的历史统计数据\n");
        sb.append("- `reverse_geocode`: 根据经纬度查找最近的摄像头/道路\n\n");

        sb.append("## 核心能力\n");
        sb.append("1. **实时路况分析** — 使用 query_traffic 查询任意道路的实时数据\n");
        sb.append("2. **趋势研判** — 基于当前数据判断交通趋势\n");
        sb.append("3. **优化建议** — 针对拥堵路段提供分流、绕行等建议\n");
        sb.append("4. **多路段对比** — 对比不同道路的交通状况\n\n");

        sb.append("## 数据指标说明\n");
        sb.append("- `count_car / count_motor / count_person` — 汽车/摩托车/行人数量\n");
        sb.append("- `speed_car / speed_motor` — 平均速度 (km/h)\n");
        sb.append("- `congestion_index` — 拥堵指数 (0~1，越高越拥堵)\n");
        sb.append("- `density_status` — 密度状态：clear(畅通)/busy(繁忙)/congested(拥堵)/offline(离线)\n");
        sb.append("- `speed_status` — 速度状态：fast(快速)/slow(缓慢)/unknown(未知)\n");
        sb.append("- `online` — 边缘节点在线状态：true(在线)/false(离线)\n\n");

        sb.append("## 数据状态解读（重要）\n");
        sb.append("- 如果 `online` 为 false 或 `density_status` 为 offline，说明该道路的边缘节点当前离线，不是 API 故障。应向用户如实说明节点离线，并主动提议查询历史数据。\n");
        sb.append("- 如果 `count_car`、`speed_car` 等均为 0 且 `online` 为 false，说明无实时数据上报，不要编造拥堵或畅通的结论。\n");
        sb.append("- 如果 tool 返回 `success: false`，根据 error 内容如实告知，不要自行猜测失败原因。\n\n");

        sb.append("## 回答规范\n");
        sb.append("- 用简洁、专业的中文回答\n");
        sb.append("- 涉及数据时使用工具查询，不要凭空猜测\n");
        sb.append("- 给出建议时说明理由和依据\n");
        sb.append("- 实时数据不可用时，主动使用 query_history 查询历史数据作为参考\n\n");

        // 注入道路列表
        List<String> roads = trafficService.roadNames();
        if (!roads.isEmpty()) {
            sb.append("## 当前监控道路列表\n");
            sb.append(String.join("、", roads));
            sb.append("\n\n");
        }

        // 如果有指定道路上下文，提示可以查询
        if (roadContext != null && !roadContext.isBlank()) {
            sb.append("用户当前关注道路：").append(roadContext).append("。你可以使用 query_traffic 查询其实时数据。\n");
        }

        return sb.toString();
    }
}
