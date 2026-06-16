package com.easyfamily.ai.chat;

import com.easyfamily.ai.llm.LlmProvider;
import com.easyfamily.ai.memory.UserMemoryService;
import com.easyfamily.security.AuthContext;
import com.easyfamily.user.dto.UserProfileDtos.UserProfile;
import com.easyfamily.user.service.UserProfileService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private static final Pattern MEMORY_SAVE_PATTERN =
            Pattern.compile("<!--MEMORY_SAVE:(\\{.*?\\})-->", Pattern.DOTALL);

    private final LlmProvider llmProvider;
    private final UserMemoryService userMemoryService;
    private final UserProfileService userProfileService;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private static final String SYSTEM_PROMPT_BODY = """

            你的能力包括：
            1. 查询手机号实名绑定 —— 帮助用户验证手机号是否与姓名/身份证匹配
            2. 手机号管理 —— 帮助用户绑定、解绑、设置主手机号
            3. 家庭成员管理 —— 添加、查看、移除大家庭成员
            4. 配额查询 —— 查看当日剩余查询次数
            5. 车辆保养管理 —— 记录保养项目与费用，分析高消费项目并给出 DIY 建议
            6. 账单管理 —— 帮助用户记录餐饮、住房、交通、购物、医疗、娱乐等日常支出

            关于车辆保养：
            - 用户可以在「我的 → 车辆」中添加车辆和保养记录
            - 保养项目分类包括：机油、刹车、轮胎、空调、滤芯、火花塞、蓄电池等
            - 统计页面可以按分类查看累计花费，高消费项目可以重点分析
            - 建议 DIY 替代时给出安全提醒（如：刹车系统不建议自行更换）
            - 如果用户询问「哪些可以自己换」，根据项目类型给出建议

            关于账单管理：
            - 当用户说类似「今天吃饭花了38元」「买菜花了120」「交了房租3000」「打车15块」时，识别为账单录入意图
            - 识别到账单意图后，在回复文字末尾（换行后）追加如下格式标记（不可修改格式）：
              <!--BILL_ACTION:{"category":"餐饮","amount":38.00,"note":"午饭","date":"2026-06-13"}-->
            - category 只能是以下值之一：餐饮、住房、交通、购物、医疗、娱乐、其他
            - date 使用当天日期（yyyy-MM-dd 格式）
            - 回复正文用友好语气确认，例如「已识别到您的餐饮支出，请确认后记录 ✅」
            - 如果金额不明确，先询问清楚再生成标记
            - 用户可以在「我的 → 账单」中查看历史账单和分类统计

            关于记忆功能：
            - 你可以记住用户的长期、稳定信息（如家庭成员构成、车辆信息、长期偏好、习惯性消费模式等），用于未来对话中提供更个性化的帮助
            - 当你从对话中识别到值得长期记住的新信息时，在回复文字末尾（换行后）追加如下格式标记（可以有多条，每条一行，不可修改格式）：
              <!--MEMORY_SAVE:{"content":"用户家里有一只叫旺财的狗","category":"family"}-->
            - category 只能是以下值之一：family（家庭成员/宠物等家庭构成信息）、vehicle（车辆相关信息）、preference（长期偏好）、habit（习惯性消费/行为模式）、other（其他）；不确定时使用 other
            - 只记录稳定、长期有用的信息，不要记录一次性的、临时的内容（例如某一次的具体账单金额）
            - 不要把记忆标记内容读给用户，标记是给系统用的
            """;

    private static final String SYSTEM_PROMPT_TONE_FOOTER = """

            回答要求：
            - 使用友好、温暖的语气，适合家庭场景
            - 回复简洁，每次不超过 200 字
            - 如果用户要执行操作但信息不全，主动引导用户提供
            - 不要编造查询结果，只说能做什么、怎么做
            """;

    public ChatController(LlmProvider llmProvider, UserMemoryService userMemoryService,
                           UserProfileService userProfileService, ObjectMapper objectMapper) {
        this.llmProvider = llmProvider;
        this.userMemoryService = userMemoryService;
        this.userProfileService = userProfileService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody Map<String, String> body) {
        String userMessage = body.getOrDefault("message", "");
        var currentUser = AuthContext.currentUser();

        UserProfile profile = userProfileService.getProfile(currentUser.userId(), currentUser.phone());
        String systemPrompt = buildSystemPrompt(profile.butlerName(), profile.butlerPersona());

        List<String> memories = userMemoryService.relevantForPrompt(currentUser.userId(), userMessage, 20);
        StringBuilder contextBuilder = new StringBuilder();
        if (!memories.isEmpty()) {
            contextBuilder.append("[关于该用户的已知记忆，可用于个性化回复，不要逐字复述给用户：\n");
            for (String memory : memories) {
                contextBuilder.append("- ").append(memory).append("\n");
            }
            contextBuilder.append("]\n");
        }
        contextBuilder.append("[用户ID: ").append(currentUser.userId())
                .append(", 当前手机号: ").append(currentUser.phone()).append("] ")
                .append(userMessage);
        String contextMessage = contextBuilder.toString();

        SseEmitter emitter = new SseEmitter(60_000L);

        executor.execute(() -> {
            try {
                String reply = llmProvider.chat(systemPrompt, contextMessage);
                reply = extractAndStripMemories(reply, currentUser.userId());
                emitter.send(SseEmitter.event().name("message").data(reply));
                emitter.complete();
            } catch (Exception e) {
                log.error("Chat stream error for user {}", currentUser.userId(), e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("AI 回复失败，请稍后重试"));
                } catch (IOException ignored) {
                }
                emitter.complete();
            }
        });

        return emitter;
    }

    /**
     * Builds the per-request system prompt, customizing the opening self-identification
     * sentence with the user's chosen butler name (falling back to "青鸟管家" for the
     * default) and appending a tone instruction based on the user's chosen persona.
     */
    private String buildSystemPrompt(String butlerName, String butlerPersona) {
        String intro;
        if (UserProfileService.DEFAULT_BUTLER_NAME.equals(butlerName)) {
            intro = "你是 easyfamily 的 AI 智能助手「青鸟管家」，帮助用户进行家庭成员手机号安全查询和管理，以及车辆保养记录管理。\n";
        } else {
            intro = "你是用户的专属AI管家\"" + butlerName + "\"，帮助用户进行家庭成员手机号安全查询和管理，以及车辆保养记录管理。\n";
        }

        String personaInstruction = switch (butlerPersona) {
            case "strict" -> "- 回复风格严谨、简洁、条理清晰，避免使用表情符号和俏皮话\n";
            case "humorous" -> "- 回复风格轻松幽默，可以适当使用俏皮的语气和表情符号，但不要影响信息准确性\n";
            default -> "";
        };

        return intro + SYSTEM_PROMPT_BODY + SYSTEM_PROMPT_TONE_FOOTER + personaInstruction;
    }

    /**
     * Finds all MEMORY_SAVE markers in the LLM reply, persists each as a memory for the
     * given user, and returns the reply with those markers stripped out. BILL_ACTION
     * markers (and any other content) are left untouched.
     */
    private String extractAndStripMemories(String reply, String userId) {
        for (MemorySaveEntry entry : extractMemoryEntries(reply)) {
            try {
                userMemoryService.add(userId, entry.content(), entry.category());
            } catch (Exception e) {
                log.warn("Failed to save memory for user {}: {}", userId, entry.content(), e);
            }
        }
        return stripMemoryMarkers(reply);
    }

    /**
     * Parses the {@code content} and {@code category} fields out of every MEMORY_SAVE
     * marker in the reply. Markers with invalid JSON are skipped (with a warning) rather
     * than failing the whole response.
     */
    private List<MemorySaveEntry> extractMemoryEntries(String reply) {
        List<MemorySaveEntry> entries = new ArrayList<>();
        if (reply == null) {
            return entries;
        }
        Matcher matcher = MEMORY_SAVE_PATTERN.matcher(reply);
        while (matcher.find()) {
            String json = matcher.group(1);
            try {
                JsonNode node = objectMapper.readTree(json);
                String content = node.path("content").asText(null);
                if (content != null && !content.isBlank()) {
                    String category = node.path("category").asText(null);
                    entries.add(new MemorySaveEntry(content, category));
                }
            } catch (Exception e) {
                log.warn("Failed to parse MEMORY_SAVE marker: {}", json, e);
            }
        }
        return entries;
    }

    private record MemorySaveEntry(String content, String category) {}

    /**
     * Removes all {@code <!--MEMORY_SAVE:...-->} markers (including DOTALL bodies) from
     * the reply, trimming the resulting trailing/leading whitespace. BILL_ACTION markers
     * are left in place for the client.
     */
    public static String stripMemoryMarkers(String reply) {
        if (reply == null) {
            return reply;
        }
        return MEMORY_SAVE_PATTERN.matcher(reply).replaceAll("").strip();
    }
}
