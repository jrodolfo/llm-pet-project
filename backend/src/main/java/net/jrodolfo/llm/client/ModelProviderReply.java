package net.jrodolfo.llm.client;

import net.jrodolfo.llm.dto.ModelProviderMetadata;

public record ModelProviderReply(
        String text,
        ModelProviderMetadata metadata
) {
}
