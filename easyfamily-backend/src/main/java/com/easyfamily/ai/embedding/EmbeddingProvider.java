package com.easyfamily.ai.embedding;

import java.util.List;
import java.util.Optional;

/**
 * Computes vector embeddings for short pieces of text, used for semantic memory
 * recall. Implementations must never throw on failure/misconfiguration — callers
 * rely on {@link Optional#empty()} to trigger a graceful fallback to non-semantic
 * (recency-based) behaviour.
 */
public interface EmbeddingProvider {

    /**
     * @param text the text to embed
     * @return the embedding vector, or {@link Optional#empty()} if embeddings are
     *         unavailable (not configured, or the call failed)
     */
    Optional<List<Float>> embed(String text);
}
