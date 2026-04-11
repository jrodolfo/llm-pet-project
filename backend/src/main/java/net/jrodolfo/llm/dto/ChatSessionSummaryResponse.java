package net.jrodolfo.llm.dto;

import java.time.Instant;

public record ChatSessionSummaryResponse(
        String sessionId,
        String title,
        String summary,
        String model,
        Instant createdAt,
        Instant updatedAt,
        int messageCount
) {
}
