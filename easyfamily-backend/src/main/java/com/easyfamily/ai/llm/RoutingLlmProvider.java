package com.easyfamily.ai.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutingLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(RoutingLlmProvider.class);
    private static final String FALLBACK_MESSAGE = "AI分析暂时不可用，请稍后查看详情。";

    private final DeepSeekLlmProvider deepSeekProvider;
    private final QwenLlmProvider qwenProvider;
    private final ClaudeLlmProvider claudeProvider;

    public RoutingLlmProvider(DeepSeekLlmProvider deepSeekProvider,
                               QwenLlmProvider qwenProvider,
                               ClaudeLlmProvider claudeProvider) {
        this.deepSeekProvider = deepSeekProvider;
        this.qwenProvider = qwenProvider;
        this.claudeProvider = claudeProvider;
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        try {
            return deepSeekProvider.chat(systemPrompt, userMessage);
        } catch (LlmUnavailableException e) {
            log.warn("DeepSeek LLM unavailable, falling back to Qwen: {}", e.getMessage());
        }
        try {
            return qwenProvider.chat(systemPrompt, userMessage);
        } catch (LlmUnavailableException e) {
            log.warn("Qwen LLM unavailable, falling back to Claude: {}", e.getMessage());
        }
        try {
            return claudeProvider.chat(systemPrompt, userMessage);
        } catch (LlmUnavailableException e) {
            log.warn("Claude LLM also unavailable, using hardcoded fallback: {}", e.getMessage());
        }
        return FALLBACK_MESSAGE;
    }
}
