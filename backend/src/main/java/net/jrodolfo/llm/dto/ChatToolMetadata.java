package net.jrodolfo.llm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tool provenance attached to assistant responses when a local MCP-backed tool was involved.")
public record ChatToolMetadata(
        boolean used,
        String name,
        String status,
        String summary
) {
}
