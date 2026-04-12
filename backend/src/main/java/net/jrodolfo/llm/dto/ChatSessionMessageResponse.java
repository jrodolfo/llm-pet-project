package net.jrodolfo.llm.dto;

import java.time.Instant;
import java.util.Map;

public record ChatSessionMessageResponse(
        String role,
        String content,
        ChatToolMetadata tool,
        Map<String, Object> toolResult,
        ModelProviderMetadata metadata,
        Instant timestamp
) {
}
