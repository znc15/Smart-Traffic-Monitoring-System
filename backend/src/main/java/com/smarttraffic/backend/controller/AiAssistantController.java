package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.model.AiChatConversationEntity;
import com.smarttraffic.backend.model.AiChatMessageEntity;
import com.smarttraffic.backend.repository.AiChatConversationRepository;
import com.smarttraffic.backend.repository.AiChatMessageRepository;
import com.smarttraffic.backend.security.CurrentUser;
import com.smarttraffic.backend.security.SecurityUtils;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/v1/ai")
public class AiAssistantController {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantController.class);
    private final LlmService llmService;
    private final AiChatConversationRepository conversationRepo;
    private final AiChatMessageRepository messageRepo;
    private final TrafficService trafficService;
    private final RedisCacheService redisCacheService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public AiAssistantController(LlmService llmService,
                                 AiChatConversationRepository conversationRepo,
                                 AiChatMessageRepository messageRepo,
                                 TrafficService trafficService,
                                 RedisCacheService redisCacheService) {
        this.llmService = llmService;
        this.conversationRepo = conversationRepo;
        this.messageRepo = messageRepo;
        this.trafficService = trafficService;
        this.redisCacheService = redisCacheService;
    }

    // ─── 对话列表（支持分页）──────────────────────────────────

    @GetMapping("/conversations")
    public Map<String, Object> listConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));

        // Try cache first (only for first page)
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

    @DeleteMapping("/conversations/{id}")
    public Map<String, String> deleteConversation(@PathVariable Long id) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        getConversationForUser(id, user.id()); // Validate ownership
        messageRepo.deleteByConversationId(id);
        conversationRepo.deleteById(id);
        redisCacheService.evictAiConversations(user.id());
        redisCacheService.evictAiMessages(id);
        return Map.of("message", "删除成功");
    }

    // ─── 清空对话消息 ──────────────────────────────────────────

    @DeleteMapping("/conversations/{id}/messages")
    public Map<String, String> clearConversationMessages(@PathVariable Long id) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        getConversationForUser(id, user.id()); // Validate ownership
        messageRepo.deleteByConversationId(id);
        redisCacheService.evictAiMessages(id);
        return Map.of("message", "已清空对话消息");
    }

    // ─── 对话消息历史 ──────────────────────────────────────────

    @GetMapping("/conversations/{id}/messages")
    public List<Map<String, Object>> getMessages(@PathVariable Long id) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        getConversationForUser(id, user.id()); // Validate ownership

        // Try cache first
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

    // ─── 流式聊天（SSE） ────────────────────────────────────────

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
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", buildSystemPrompt(conv.getRoadContext())));
        for (var m : history) {
            messages.add(Map.of("role", m.getRole(), "content", m.getContent()));
        }

        // 首条消息时先用截取设置初始标题，流完成后异步替换为 LLM 生成的标题
        final boolean needsTitle = history.size() <= 2;
        if (needsTitle) {
            String fallbackTitle = userMessage.length() > 30 ? userMessage.substring(0, 30) + "..." : userMessage;
            conv.setTitle(fallbackTitle);
            conversationRepo.save(conv);
        }

        SseEmitter emitter = new SseEmitter(120_000L);
        StringBuilder fullResponse = new StringBuilder();

        executor.execute(() -> {
            try {
                llmService.streamChat(messages, chunk -> {
                    try {
                        fullResponse.append(chunk);
                        emitter.send(SseEmitter.event().name("chunk").data(chunk));
                    } catch (Exception e) {
                        log.warn("SSE 发送 chunk 失败: {}", e.getMessage());
                    }
                });
                // 保存助手回复
                AiChatMessageEntity assistantMsg = new AiChatMessageEntity();
                assistantMsg.setConversationId(id);
                assistantMsg.setRole("assistant");
                assistantMsg.setContent(fullResponse.toString());
                messageRepo.save(assistantMsg);
                redisCacheService.evictAiMessages(id);
                redisCacheService.evictAiConversations(conv.getUserId());

                // Async title generation (fire-and-forget, won't block SSE)
                if (needsTitle) {
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
                            log.warn("异步标题生成失败，保留截取标题: {}", ex.getMessage());
                        }
                    });
                }

                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                log.error("AI 聊天异常: {}", e.getMessage());
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

        // 基础角色与能力定义
        sb.append("你是「智能交通监控系统」的 AI 助手，拥有以下能力：\n\n");
        sb.append("## 核心能力\n");
        sb.append("1. **实时路况分析** — 查询任意道路的当前车流量、车速、拥堵指数、密度状态等实时数据\n");
        sb.append("2. **趋势研判** — 基于当前数据判断交通趋势，预测短期拥堵变化\n");
        sb.append("3. **优化建议** — 针对拥堵路段提供分流、信号灯优化、绕行等建议\n");
        sb.append("4. **异常诊断** — 分析离线节点、车速骤降、密度异常等问题的可能原因\n");
        sb.append("5. **多路段对比** — 对比不同道路的交通状况，辅助调度决策\n\n");

        // 数据指标说明
        sb.append("## 数据指标说明\n");
        sb.append("- `count_car / count_motor / count_person` — 当前检测到的汽车/摩托车/行人数量\n");
        sb.append("- `speed_car / speed_motor` — 汽车/摩托车的平均速度 (km/h)\n");
        sb.append("- `congestion_index` — 拥堵指数 (0~1，越高越拥堵)\n");
        sb.append("- `density_status` — 密度状态：`clear`(畅通) / `busy`(繁忙) / `congested`(拥堵) / `offline`(离线)\n");
        sb.append("- `speed_status` — 速度状态：`fast`(快速) / `slow`(缓慢) / `unknown`(未知)\n\n");

        // 交互规范
        sb.append("## 回答规范\n");
        sb.append("- 用简洁、专业的中文回答\n");
        sb.append("- 涉及数据时引用具体数值，不要凭空猜测\n");
        sb.append("- 如果用户没有指定道路，优先推荐当前拥堵最严重的路段\n");
        sb.append("- 给出建议时说明理由和依据\n\n");

        // 注入全部道路列表
        List<String> roads = trafficService.roadNames();
        if (!roads.isEmpty()) {
            sb.append("## 当前监控道路列表\n");
            sb.append(String.join("、", roads));
            sb.append("\n\n");
        }

        // 注入指定道路的实时数据
        if (roadContext != null && !roadContext.isBlank()) {
            sb.append("## 当前关注道路的实时数据：").append(roadContext).append("\n");
            try {
                Map<String, Object> info = trafficService.info(roadContext);
                sb.append("- 在线状态：").append(info.get("online")).append("\n");
                sb.append("- 汽车数量：").append(info.get("count_car")).append("\n");
                sb.append("- 摩托车数量：").append(info.get("count_motor")).append("\n");
                sb.append("- 行人数量：").append(info.get("count_person")).append("\n");
                sb.append("- 汽车平均速度：").append(info.get("speed_car")).append(" km/h\n");
                sb.append("- 摩托车平均速度：").append(info.get("speed_motor")).append(" km/h\n");
                sb.append("- 拥堵指数：").append(info.get("congestion_index")).append("\n");
                sb.append("- 密度状态：").append(info.get("density_status")).append("\n");
                sb.append("- 速度状态：").append(info.get("speed_status")).append("\n");
            } catch (Exception e) {
                sb.append("（未能获取该道路的实时数据，可能道路名称不在监控范围内）\n");
            }
            sb.append("\n请基于以上实时数据回答用户的问题。\n");
        }

        return sb.toString();
    }
}