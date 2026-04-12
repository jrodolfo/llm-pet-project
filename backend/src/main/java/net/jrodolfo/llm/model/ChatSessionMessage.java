package net.jrodolfo.llm.model;

import net.jrodolfo.llm.dto.ChatToolMetadata;
import net.jrodolfo.llm.dto.ModelProviderMetadata;

import java.time.Instant;

public record ChatSessionMessage(
        String role,
        String content,
        ChatToolMetadata tool,
        ModelProviderMetadata metadata,
        Instant timestamp
) {
}
