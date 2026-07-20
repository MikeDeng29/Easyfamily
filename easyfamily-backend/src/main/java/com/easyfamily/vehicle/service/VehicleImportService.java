package com.easyfamily.vehicle.service;

import com.easyfamily.vehicle.dto.VehicleDtos.MaintenanceImportResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VehicleImportService {

    private static final Logger log = LoggerFactory.getLogger(VehicleImportService.class);
    private static final String ENDPOINT = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-haiku-4-5-20251001";
    private static final Pattern MARKDOWN_CODE_BLOCK = Pattern.compile(
            "```(?:json)?\\s*(\\{[\\s\\S]*?\\})\\s*```", Pattern.DOTALL);

    private static final String EXTRACTION_PROMPT =
            "你是一个保养单信息提取助手。请从图片中提取车辆保养信息，返回以下 JSON 格式（无法识别的字段填 null，items 数组可为空）：\n"
            + "{\"plateNumber\":\"...\",\"brand\":\"...\",\"model\":\"...\",\"year\":2023,"
            + "\"serviceDate\":\"2024-01-15\",\"mileageKm\":50000,\"shopName\":\"...\","
            + "\"notes\":\"...\",\"items\":[{\"category\":\"机油\",\"itemName\":\"全合成机油\",\"cost\":280.0}]}\n"
            + "category 只能从以下选项中选择：机油、刹车、轮胎、空调、滤芯、火花塞、蓄电池、其他\n"
            + "只返回 JSON，不要有任何其他文字。";

    private final RestTemplate llmRestTemplate;
    private final ObjectMapper objectMapper;
    private final String claudeApiKey;

    public VehicleImportService(RestTemplate llmRestTemplate, ObjectMapper objectMapper, String claudeApiKey) {
        this.llmRestTemplate = llmRestTemplate;
        this.objectMapper = objectMapper;
        this.claudeApiKey = claudeApiKey;
    }

    public MaintenanceImportResult importFromImage(MultipartFile image) throws IOException {
        if (claudeApiKey == null || claudeApiKey.isBlank()) {
            throw new IllegalStateException("Claude API key not configured");
        }

        byte[] imageBytes = image.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String mediaType = resolveMediaType(image.getContentType());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", claudeApiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> imageSource = Map.of(
                "type", "base64",
                "media_type", mediaType,
                "data", base64Image
        );
        Map<String, Object> imageContent = Map.of(
                "type", "image",
                "source", imageSource
        );
        Map<String, Object> textContent = Map.of(
                "type", "text",
                "text", EXTRACTION_PROMPT
        );
        Map<String, Object> message = Map.of(
                "role", "user",
                "content", List.of(imageContent, textContent)
        );
        Map<String, Object> body = Map.of(
                "model", MODEL,
                "max_tokens", 1024,
                "messages", List.of(message)
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = llmRestTemplate.exchange(ENDPOINT, HttpMethod.POST, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Claude Vision API returned HTTP " + response.getStatusCode().value());
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        String rawText = root.path("content").path(0).path("text").asText();
        String jsonText = stripMarkdownCodeBlock(rawText);

        log.debug("Claude Vision extracted JSON: {}", jsonText);
        return objectMapper.readValue(jsonText, MaintenanceImportResult.class);
    }

    private static String resolveMediaType(String contentType) {
        if (contentType != null && contentType.equalsIgnoreCase("image/png")) {
            return "image/png";
        }
        return "image/jpeg";
    }

    private static String stripMarkdownCodeBlock(String text) {
        if (text == null) return "{}";
        String trimmed = text.strip();
        Matcher matcher = MARKDOWN_CODE_BLOCK.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1).strip();
        }
        return trimmed;
    }
}
