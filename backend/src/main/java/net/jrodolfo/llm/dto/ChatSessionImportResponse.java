package net.jrodolfo.llm.dto;

public record ChatSessionImportResponse(
        String sessionId,
        String title,
        String summary,
        boolean idChanged,
        int messageCount
) {
}
