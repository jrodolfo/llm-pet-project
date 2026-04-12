package net.jrodolfo.llm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Metadata event payload emitted by the streaming chat SSE endpoint.")
public record ChatStreamMetadata(
        @Schema(description = "Active session identifier for the streaming request.", example = "session-123")
        String sessionId,
        @Schema(description = "Optional tool provenance attached to the stream.")
        ChatToolMetadata tool,
        @Schema(description = "Optional structured tool result payload attached to the stream.")
        Map<String, Object> toolResult,
        @Schema(description = "Pending clarification state carried on the stream when more user input is needed.")
        PendingToolCallResponse pendingTool,
        @Schema(description = "Optional final model provider metadata emitted near the end of the stream.")
        ModelProviderMetadata metadata
) {
}
