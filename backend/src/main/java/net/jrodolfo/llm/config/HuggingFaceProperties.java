package net.jrodolfo.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "huggingface")
public record HuggingFaceProperties(
        String baseUrl,
        String apiToken,
        String defaultModel,
        List<String> models,
        int connectTimeoutSeconds,
        int readTimeoutSeconds
) {
}
