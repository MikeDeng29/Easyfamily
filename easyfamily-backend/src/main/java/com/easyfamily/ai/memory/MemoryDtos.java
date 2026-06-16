package com.easyfamily.ai.memory;

public final class MemoryDtos {

    private MemoryDtos() {}

    public record MemoryItem(
            Long id,
            String content,
            String category,
            long createdAt
    ) {}
}
