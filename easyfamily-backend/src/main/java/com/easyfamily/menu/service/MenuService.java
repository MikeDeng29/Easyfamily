package com.easyfamily.menu.service;

import com.easyfamily.ai.llm.LlmProvider;
import com.easyfamily.menu.dto.MenuDtos.WeeklyMenuResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
public class MenuService {

    private static final Logger log = LoggerFactory.getLogger(MenuService.class);
    private static final int TOP_DISH_LIMIT = 8;

    private final LlmProvider llmProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbc;

    public MenuService(LlmProvider llmProvider,
                       RedisTemplate<String, String> redisTemplate,
                       ObjectMapper objectMapper,
                       JdbcTemplate jdbc) {
        this.llmProvider = llmProvider;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.jdbc = jdbc;
    }

    /** Returns this week's menu for the given user, generating via LLM on cache miss. */
    public WeeklyMenuResponse getWeeklyMenu(String userId, String city) {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return getWeeklyMenuForWeek(userId, city, monday);
    }

    /** Generates next week's menu for a specific user — called by the scheduler. */
    public WeeklyMenuResponse generateNextWeek(String userId, String city) {
        LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(1);
        return getWeeklyMenuForWeek(userId, city, nextMonday);
    }

    /**
     * Returns this week's menu for the given user without a redundant DB SELECT on cache hits.
     * On cache miss, looks up the user's city directly, then generates via LLM.
     */
    public WeeklyMenuResponse getWeeklyMenuForUser(String userId) {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        String cacheKey = "menu:weekly:" + userId + ":" + monday;
        // Try cache — no DB needed
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) return objectMapper.readValue(cached, WeeklyMenuResponse.class);
        } catch (Exception e) {
            log.warn("Cache read failed for {}: {}", cacheKey, e.getMessage());
        }
        // Cache miss — look up city then generate
        String city = getUserCity(userId);
        return getWeeklyMenuForWeek(userId, city, monday);
    }

    /** Upserts a dish preference: inserts with weight=1, or increments weight on conflict (once per calendar day). */
    public void incrementDishWeight(String userId, String dishName) {
        String trimmed = sanitizeDishName(dishName == null ? "" : dishName);
        if (trimmed.isEmpty() || trimmed.length() > 200) return;
        jdbc.update("""
                INSERT INTO dish_preferences (user_id, dish_name, weight)
                VALUES (?, ?, 1)
                ON DUPLICATE KEY UPDATE weight = IF(DATE(updated_at) < CURDATE(), weight + 1, weight), updated_at = IF(DATE(updated_at) < CURDATE(), CURRENT_TIMESTAMP, updated_at)
                """, userId, trimmed);
    }

    /** Retrieves all active user IDs and their cities for bulk scheduling. */
    public List<String[]> getAllUsersWithCity() {
        // Only pre-generate for users who have set a city; others get on-demand generation.
        return jdbc.query(
                "SELECT user_id, city FROM users WHERE user_id IS NOT NULL AND city IS NOT NULL AND city != ''",
                (rs, n) -> new String[]{rs.getString("user_id"), rs.getString("city")}
        );
    }

    // -------------------------------------------------------------------------

    private WeeklyMenuResponse getWeeklyMenuForWeek(String userId, String city, LocalDate monday) {
        String effectiveCity = (city == null || city.isBlank()) ? "全国" : city.trim();
        String weekOf = monday.toString();
        String cacheKey = "menu:weekly:" + userId + ":" + weekOf;

        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, WeeklyMenuResponse.class);
            }
        } catch (Exception e) {
            log.warn("Cache read failed for {}: {}", cacheKey, e.getMessage());
        }

        List<String> topDishes = getUserTopDishes(userId);
        String season = determineSeason(monday.getMonthValue());
        String prompt = buildPrompt(effectiveCity, weekOf, season, topDishes);

        String llmResponse = llmProvider.chat(prompt, "");
        WeeklyMenuResponse menu = parseMenuResponse(llmResponse);

        try {
            String json = objectMapper.writeValueAsString(menu);
            redisTemplate.opsForValue().set(cacheKey, json, Duration.ofDays(7));
        } catch (Exception e) {
            log.warn("Cache write failed for {}: {}", cacheKey, e.getMessage());
        }

        return menu;
    }

    private List<String> getUserTopDishes(String userId) {
        try {
            return jdbc.query(
                    "SELECT dish_name FROM dish_preferences WHERE user_id = ? ORDER BY weight DESC LIMIT ?",
                    (rs, n) -> rs.getString("dish_name"),
                    userId, TOP_DISH_LIMIT
            );
        } catch (Exception e) {
            log.warn("Failed to load dish preferences for {}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    private String determineSeason(int month) {
        if (month >= 3 && month <= 5) return "春季";
        if (month >= 6 && month <= 8) return "夏季";
        if (month >= 9 && month <= 11) return "秋季";
        return "冬季";
    }

    private String buildPrompt(String city, String weekOf, String season, List<String> topDishes) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的家庭营养师，请严格遵照《中国居民膳食指南（2022）》为").append(city)
          .append("家庭生成本周（").append(weekOf).append("开始的一周）的菜单计划。\n\n");

        sb.append("【膳食指南核心准则】\n")
          .append("1. 食物多样，谷类为主——每天至少12种食物，全周25种以上；安排1-2次粗粮/杂粮\n")
          .append("2. 多蔬果、奶类、大豆——每天蔬菜≥300g，其中深色蔬菜占一半；每周安排豆制品≥3次\n")
          .append("3. 适量鱼禽蛋瘦肉——红肉（猪牛羊）每周≤3次，优先安排鱼虾≥2次\n")
          .append("4. 少盐少油控糖——烹饪方式优先蒸、煮、炖、拌，减少油炸\n")
          .append("5. 早餐营养充足——必须包含谷物+蛋白质（蛋/奶/豆）+果蔬\n\n");

        sb.append("【地域与季节】当前季节：").append(season)
          .append("，请重点推荐").append(city).append("当季新鲜时蔬。\n\n");

        if (!topDishes.isEmpty()) {
            sb.append("【用户偏好】该家庭喜欢以下菜品，请在本周菜单中适当安排（不必全部出现，可做变化）：\n");
            topDishes.forEach(d -> sb.append("- ").append(sanitizeDishName(d)).append("\n"));
            sb.append("\n");
        }

        sb.append("请返回严格的JSON格式（不要包含任何其他文字），格式如下：\n")
          .append("{\n")
          .append("  \"weekOf\": \"").append(weekOf).append("\",\n")
          .append("  \"city\": \"").append(city).append("\",\n")
          .append("  \"days\": [\n")
          .append("    {\n")
          .append("      \"date\": \"YYYY-MM-DD\",\n")
          .append("      \"dayLabel\": \"周一\",\n")
          .append("      \"breakfast\": \"早餐描述（1句话，需含谷物+蛋白质）\",\n")
          .append("      \"lunch\": \"午餐描述（1句话）\",\n")
          .append("      \"dinner\": \"晚餐描述（1句话）\",\n")
          .append("      \"keyVegetables\": [\"时蔬1\", \"时蔬2\"]\n")
          .append("    },\n")
          .append("    ... (共7天，周一到周日)\n")
          .append("  ],\n")
          .append("  \"seasonTip\": \"当季饮食小贴士（1-2句话）\"\n")
          .append("}");

        return sb.toString();
    }

    private WeeklyMenuResponse parseMenuResponse(String llmResponse) {
        String json = extractJson(llmResponse);
        try {
            return objectMapper.readValue(json, WeeklyMenuResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse weekly menu JSON: " + e.getMessage(), e);
        }
    }

    private String getUserCity(String userId) {
        List<String> cities = jdbc.query(
                "SELECT city FROM users WHERE user_id = ? LIMIT 1",
                (rs, n) -> rs.getString("city"), userId);
        String city = cities.isEmpty() ? null : cities.get(0);
        return (city == null || city.isBlank()) ? "全国" : city.trim();
    }

    private String sanitizeDishName(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("[\\p{Cntrl}\\n\\r`#*\\[\\]]", "").trim();
    }

    static String extractJson(String raw) {
        if (raw == null) return "{}";
        String stripped = raw.replaceAll("```(?:json)?\\s*", "").replaceAll("```", "").trim();
        int start = stripped.indexOf('{');
        int end = stripped.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return stripped.substring(start, end + 1);
        }
        return stripped;
    }
}
