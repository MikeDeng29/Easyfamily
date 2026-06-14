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

public class DeepSeekLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekLlmProvider.class);
    private static final String ENDPOINT = "https://api.deepseek.com/chat/completions";

    private final RestTemplate llmRestTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public DeepSeekLlmProvider(RestTemplate llmRestTemplate, ObjectMapper objectMapper,
                                String apiKey, String model) {
        this.llmRestTemplate = llmRestTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new LlmUnavailableException("DeepSeek API key not configured");
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userMessage)
                    )
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = llmRestTemplate.exchange(ENDPOINT, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new LlmUnavailableException("DeepSeek returned HTTP " + response.getStatusCode().value());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").path(0).path("message").path("content").asText();
        } catch (LlmUnavailableException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("DeepSeek API call failed: {}", e.getMessage());
            throw new LlmUnavailableException("DeepSeek request failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.warn("DeepSeek response parse failed: {}", e.getMessage());
            throw new LlmUnavailableException("DeepSeek response parse failed: " + e.getMessage(), e);
        }
    }
}
