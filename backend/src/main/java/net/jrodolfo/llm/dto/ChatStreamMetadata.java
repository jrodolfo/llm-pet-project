package net.jrodolfo.llm.dto;

import java.util.Map;

public record ChatStreamMetadata(
        String sessionId,
        ChatToolMetadata tool,
        Map<String, Object> toolResult,
        PendingToolCallResponse pendingTool,
        ModelProviderMetadata metadata
) {
}
