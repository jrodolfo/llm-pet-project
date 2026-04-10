package net.jrodolfo.llm.dto;

public record ChatToolMetadata(
        boolean used,
        String name,
        String status,
        String summary
) {
}
