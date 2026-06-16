package com.easyfamily.ai.chat;

import com.easyfamily.ai.llm.LlmProvider;
import com.easyfamily.ai.memory.UserMemoryService;
import com.easyfamily.bill.service.BillService;
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
import java.time.YearMonth;
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
    private final BillService billService;
    private final PromptProperties prompts;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public ChatController(LlmProvider llmProvider, UserMemoryService userMemoryService,
                           UserProfileService userProfileService, BillService billService,
                           PromptProperties prompts, ObjectMapper objectMapper) {
        this.llmProvider = llmProvider;
        this.userMemoryService = userMemoryService;
        this.userProfileService = userProfileService;
        this.billService = billService;
        this.prompts = prompts;
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

        // Inject current-month bill summary so the AI can answer spending queries
        try {
            String currentMonth = YearMonth.now().toString(); // yyyy-MM
            var stats = billService.stats(currentUser.userId(), currentMonth);
            var recent = billService.list(currentUser.userId(), currentMonth);
            contextBuilder.append("[本月账单数据 (").append(currentMonth).append(")：\n");
            contextBuilder.append("  收入: ").append(stats.totalIncome()).append(" 元")
                    .append("，支出: ").append(stats.totalExpense()).append(" 元")
                    .append("，净结余: ").append(stats.netSavings()).append(" 元")
                    .append("，共 ").append(stats.count()).append(" 笔\n");
            if (!stats.byCategory().isEmpty()) {
                contextBuilder.append("  分类明细：\n");
                for (var cat : stats.byCategory()) {
                    contextBuilder.append("    - [").append(cat.direction()).append("] ")
                            .append(cat.category()).append(": ").append(cat.amount())
                            .append(" 元 (").append(cat.count()).append(" 笔)\n");
                }
            }
            if (!recent.isEmpty()) {
                contextBuilder.append("  最近账单记录：\n");
                recent.stream().limit(10).forEach(b -> {
                    String noteStr = (b.note() != null && !b.note().isBlank()) ? " (" + b.note() + ")" : "";
                    contextBuilder.append("    - ").append(b.billedAt()).append(" [").append(b.direction()).append("] ")
                            .append(b.category()).append(" ").append(b.amount()).append(" 元").append(noteStr).append("\n");
                });
            }
            contextBuilder.append("]\n");
        } catch (Exception e) {
            log.debug("Could not load bill context for user {}: {}", currentUser.userId(), e.getMessage());
        }

        contextBuilder.append("[用户ID: ").append(currentUser.userId())
                .append(", 当前手机号: ").append(currentUser.phone());
        if (profile.nickname() != null && !profile.nickname().isBlank()) {
            contextBuilder.append(", 用户昵称: ").append(profile.nickname());
        }
        contextBuilder.append("] ").append(userMessage);
        String contextMessage = contextBuilder.toString();

        SseEmitter emitter = new SseEmitter(60_000L);

        executor.execute(() -> {
            try {
                String reply = llmProvider.chat(systemPrompt, contextMessage);
                reply = extractAndStripMemories(reply, currentUser.userId());
                reply = stripUnrenderableChars(reply);
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
            intro = prompts.getIntroDefault() + "\n";
        } else {
            intro = prompts.getIntroCustom().replace("{butlerName}", butlerName) + "\n";
        }

        String personaInstruction = switch (butlerPersona) {
            case "strict" -> prompts.getPersonaStrict() + "\n";
            case "humorous" -> prompts.getPersonaHumorous() + "\n";
            default -> prompts.getPersonaWarm() + "\n";
        };

        return intro + "\n"
                + prompts.getCapabilities() + "\n"
                + prompts.getModuleVehicle() + "\n"
                + prompts.getModuleBill() + "\n"
                + prompts.getModuleMemory() + "\n"
                + prompts.getToneFooter() + "\n"
                + personaInstruction;
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
     * Strips characters that iOS cannot render, preventing [?] tofu boxes:
     * - All SMP code points (U+10000+), which includes most emoji
     * - U+FFFD replacement character (encoding artifacts)
     * - Variation selectors U+FE00-U+FE0F (emoji presentation selectors)
     * - Zero-width / invisible formatting chars
     */
    private static String stripUnrenderableChars(String s) {
        if (s == null) return null;
        // Keep only BMP code points that are safe to render on iOS:
        // - exclude SMP (>= U+10000): all modern emoji live here
        // - exclude U+FFFD: Unicode replacement character (shows as [?])
        // - exclude U+FE00-U+FE0F: variation selectors (emoji presentation)
        // - exclude U+200B/200C/200D/FEFF: zero-width / BOM chars
        return s.codePoints()
                .filter(cp -> cp < 0x10000
                        && cp != 0xFFFD
                        && !(cp >= 0xFE00 && cp <= 0xFE0F)
                        && cp != 0x200B && cp != 0x200C && cp != 0x200D
                        && cp != 0xFEFF)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

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
