package net.jrodolfo.llm.dto;

import java.util.Map;

public record ChatResponse(
        String response,
        String model,
        ChatToolMetadata tool,
        Map<String, Object> toolResult,
        String sessionId,
        PendingToolCallResponse pendingTool,
        ModelProviderMetadata metadata
) {
}
