package net.jrodolfo.llm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Pending tool clarification state exposed to the frontend.")
public record PendingToolCallResponse(
        String toolName,
        String reason,
        List<String> missingFields
) {
}
