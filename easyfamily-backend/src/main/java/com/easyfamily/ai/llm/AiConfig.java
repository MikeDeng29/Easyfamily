package com.easyfamily.ai.llm;

import com.easyfamily.ai.chat.PromptProperties;
import com.easyfamily.ai.embedding.QwenEmbeddingProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(PromptProperties.class)
public class AiConfig {

    @Value("${easyfamily.ai.deepseek.api-key:}")
    private String deepSeekApiKey;

    @Value("${easyfamily.ai.deepseek.model:deepseek-chat}")
    private String deepSeekModel;

    @Value("${easyfamily.ai.deepseek.timeout-ms:15000}")
    private long deepSeekTimeoutMs;

    @Value("${easyfamily.ai.qwen.api-key:}")
    private String qwenApiKey;

    @Value("${easyfamily.ai.qwen.model:qwen-turbo}")
    private String qwenModel;

    @Value("${easyfamily.ai.qwen.timeout-ms:10000}")
    private long qwenTimeoutMs;

    @Value("${easyfamily.ai.qwen.embedding-model:text-embedding-v2}")
    private String qwenEmbeddingModel;

    @Value("${easyfamily.ai.claude.api-key:}")
    private String claudeApiKey;

    @Value("${easyfamily.ai.claude.timeout-ms:15000}")
    private long claudeTimeoutMs;

    @Bean("llmRestTemplate")
    public RestTemplate llmRestTemplate(RestTemplateBuilder builder) {
        long connectMs = 5000;
        long readMs = Math.max(deepSeekTimeoutMs, Math.max(qwenTimeoutMs, claudeTimeoutMs));
        return builder
                .connectTimeout(Duration.ofMillis(connectMs))
                .readTimeout(Duration.ofMillis(readMs))
                .build();
    }

    @Bean
    public DeepSeekLlmProvider deepSeekLlmProvider(
            RestTemplate llmRestTemplate,
            ObjectMapper objectMapper
    ) {
        return new DeepSeekLlmProvider(llmRestTemplate, objectMapper, deepSeekApiKey, deepSeekModel);
    }

    @Bean
    public QwenLlmProvider qwenLlmProvider(
            RestTemplate llmRestTemplate,
            ObjectMapper objectMapper
    ) {
        return new QwenLlmProvider(llmRestTemplate, objectMapper, qwenApiKey, qwenModel);
    }

    @Bean
    public ClaudeLlmProvider claudeLlmProvider(
            RestTemplate llmRestTemplate,
            ObjectMapper objectMapper
    ) {
        return new ClaudeLlmProvider(llmRestTemplate, objectMapper, claudeApiKey);
    }

    @Bean
    public QwenEmbeddingProvider qwenEmbeddingProvider(
            RestTemplate llmRestTemplate,
            ObjectMapper objectMapper
    ) {
        return new QwenEmbeddingProvider(llmRestTemplate, objectMapper, qwenApiKey, qwenEmbeddingModel);
    }

    @Bean
    @Primary
    public RoutingLlmProvider routingLlmProvider(
            DeepSeekLlmProvider deepSeekLlmProvider,
            QwenLlmProvider qwenLlmProvider,
            ClaudeLlmProvider claudeLlmProvider
    ) {
        return new RoutingLlmProvider(deepSeekLlmProvider, qwenLlmProvider, claudeLlmProvider);
    }
}
