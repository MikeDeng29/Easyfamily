package com.easyfamily.ai.embedding;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Embedding provider backed by DashScope's OpenAI-compatible embeddings endpoint
 * (Qwen text-embedding models). Reuses the {@code easyfamily.ai.qwen.api-key}
 * credential and the shared {@code llmRestTemplate}.
 *
 * <p>Never throws: any misconfiguration or remote failure results in
 * {@link Optional#empty()} so callers can fall back to non-semantic behaviour.
 */
public class QwenEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(QwenEmbeddingProvider.class);
    private static final String ENDPOINT = "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings";

    private final RestTemplate llmRestTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public QwenEmbeddingProvider(RestTemplate llmRestTemplate, ObjectMapper objectMapper, String apiKey, String model) {
        this.llmRestTemplate = llmRestTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public Optional<List<Float>> embed(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> body = Map.of(
                    "model", model,
                    "input", text
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = llmRestTemplate.exchange(ENDPOINT, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Qwen embedding returned HTTP {}", response.getStatusCode().value());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode dataArray = root.path("data");
            if (!dataArray.isArray() || dataArray.isEmpty()) {
                log.warn("Qwen embedding response missing data array");
                return Optional.empty();
            }
            JsonNode embeddingNode = dataArray.get(0).path("embedding");
            if (!embeddingNode.isArray() || embeddingNode.isEmpty()) {
                log.warn("Qwen embedding response missing embedding vector");
                return Optional.empty();
            }
            List<Float> vector = new ArrayList<>(embeddingNode.size());
            for (JsonNode v : embeddingNode) {
                vector.add(v.floatValue());
            }
            return Optional.of(vector);
        } catch (RestClientException e) {
            log.warn("Qwen embedding request failed: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Qwen embedding response parse failed: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
