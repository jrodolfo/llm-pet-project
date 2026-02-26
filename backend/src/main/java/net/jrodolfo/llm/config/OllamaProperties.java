package net.jrodolfo.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ollama")
public record OllamaProperties(
        String baseUrl,
        String defaultModel,
        int connectTimeoutSeconds,
        int readTimeoutSeconds
) {
}
