package com.easyfamily.ai.llm;

public interface LlmProvider {
    String chat(String systemPrompt, String userMessage);
}
