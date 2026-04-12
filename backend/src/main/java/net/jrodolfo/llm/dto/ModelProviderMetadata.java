package net.jrodolfo.llm.dto;

public record ModelProviderMetadata(
        String provider,
        String modelId,
        String stopReason,
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens,
        Long durationMs,
        Long providerLatencyMs
) {
}
