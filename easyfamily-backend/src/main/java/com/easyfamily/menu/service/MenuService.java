package com.easyfamily.menu.service;

import com.easyfamily.ai.llm.LlmProvider;
import com.easyfamily.menu.dto.MenuDtos.WeeklyMenuResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Service
public class MenuService {

    private static final Logger log = LoggerFactory.getLogger(MenuService.class);

    private final LlmProvider llmProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public MenuService(LlmProvider llmProvider,
                       RedisTemplate<String, String> redisTemplate,
                       ObjectMapper objectMapper) {
        this.llmProvider = llmProvider;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Returns a weekly menu for the given city. Results are cached in Redis for 7 days
     * keyed by city and the Monday of the current week. A null or blank city defaults to
     * "全国" so the LLM produces a nationally applicable menu.
     */
    public WeeklyMenuResponse getWeeklyMenu(String city) {
        String effectiveCity = (city == null || city.isBlank()) ? "全国" : city.trim();

        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        String weekOf = monday.toString(); // "2026-07-07"

        String cacheKey = "menu:weekly:" + effectiveCity + ":" + weekOf;

        // Check Redis cache first
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, WeeklyMenuResponse.class);
            }
        } catch (Exception e) {
            log.warn("Failed to read weekly menu from cache for key {}: {}", cacheKey, e.getMessage());
        }

        // Cache miss — call LLM
        String season = determineSeason(monday.getMonthValue());
        String prompt = buildPrompt(effectiveCity, weekOf, season);

        String llmResponse = llmProvider.chat(prompt, "");
        WeeklyMenuResponse menu = parseMenuResponse(llmResponse);

        // Write to cache with 7-day TTL; cache failure is non-fatal
        try {
            String json = objectMapper.writeValueAsString(menu);
            redisTemplate.opsForValue().set(cacheKey, json, Duration.ofDays(7));
        } catch (Exception e) {
            log.warn("Failed to cache weekly menu for key {}: {}", cacheKey, e.getMessage());
        }

        return menu;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private String determineSeason(int month) {
        if (month >= 3 && month <= 5) return "春季";
        if (month >= 6 && month <= 8) return "夏季";
        if (month >= 9 && month <= 11) return "秋季";
        return "冬季";
    }

    private String buildPrompt(String city, String weekOf, String season) {
        return "你是一位专业的家庭营养师，请为" + city + "家庭生成本周（" + weekOf + "开始的一周）的菜单计划。\n"
                + "当前季节：" + season + "（根据月份判断）。请重点推荐该城市当季新鲜时蔬。\n\n"
                + "请返回严格的JSON格式（不要包含任何其他文字），格式如下：\n"
                + "{\n"
                + "  \"weekOf\": \"" + weekOf + "\",\n"
                + "  \"city\": \"" + city + "\",\n"
                + "  \"days\": [\n"
                + "    {\n"
                + "      \"date\": \"YYYY-MM-DD\",\n"
                + "      \"dayLabel\": \"周一\",\n"
                + "      \"breakfast\": \"早餐描述（1句话）\",\n"
                + "      \"lunch\": \"午餐描述（1句话）\",\n"
                + "      \"dinner\": \"晚餐描述（1句话）\",\n"
                + "      \"keyVegetables\": [\"时蔬1\", \"时蔬2\"]\n"
                + "    },\n"
                + "    ... (共7天，周一到周日)\n"
                + "  ],\n"
                + "  \"seasonTip\": \"当季饮食小贴士（1-2句话）\"\n"
                + "}";
    }

    /**
     * Extracts the JSON object from an LLM response that may be wrapped in a markdown
     * code fence (```json ... ```) or contain surrounding text. Parses and returns the
     * {@link WeeklyMenuResponse}.
     *
     * @throws RuntimeException if the response cannot be parsed as a valid menu
     */
    private WeeklyMenuResponse parseMenuResponse(String llmResponse) {
        String json = extractJson(llmResponse);
        try {
            return objectMapper.readValue(json, WeeklyMenuResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse weekly menu JSON from LLM response: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts the outermost JSON object from a string. Handles markdown code fences
     * (```json ... ```) by stripping them before searching. Falls back to the raw
     * response if no braces are found.
     */
    static String extractJson(String raw) {
        if (raw == null) return "{}";

        // Strip markdown code fences
        String stripped = raw.replaceAll("```(?:json)?\\s*", "").replaceAll("```", "").trim();

        int start = stripped.indexOf('{');
        int end = stripped.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return stripped.substring(start, end + 1);
        }
        return stripped;
    }
}
