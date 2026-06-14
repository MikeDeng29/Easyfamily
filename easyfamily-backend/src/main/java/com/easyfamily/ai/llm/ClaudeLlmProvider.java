package com.easyfamily.ai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

public class ClaudeLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(ClaudeLlmProvider.class);
    private static final String ENDPOINT = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-haiku-4-5-20251001";

    private final RestTemplate llmRestTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public ClaudeLlmProvider(RestTemplate llmRestTemplate, ObjectMapper objectMapper, String apiKey) {
        this.llmRestTemplate = llmRestTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new LlmUnavailableException("Claude API key not configured");
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            Map<String, Object> body = Map.of(
                    "model", MODEL,
                    "max_tokens", 512,
                    "system", systemPrompt,
                    "messages", List.of(
                            Map.of("role", "user", "content", userMessage)
                    )
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = llmRestTemplate.exchange(ENDPOINT, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new LlmUnavailableException("Claude returned HTTP " + response.getStatusCode().value());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("content").path(0).path("text").asText();
        } catch (LlmUnavailableException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("Claude API call failed: {}", e.getMessage());
            throw new LlmUnavailableException("Claude request failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.warn("Claude response parse failed: {}", e.getMessage());
            throw new LlmUnavailableException("Claude response parse failed: " + e.getMessage(), e);
        }
    }
}
