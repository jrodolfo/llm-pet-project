package net.jrodolfo.llm.config;

import net.jrodolfo.llm.client.OllamaClient;
import net.jrodolfo.llm.provider.ChatModelProvider;
import net.jrodolfo.llm.provider.OllamaChatModelProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelProviderConfig {

    @Bean
    public ChatModelProvider chatModelProvider(AppModelProperties appModelProperties, OllamaClient ollamaClient) {
        String provider = appModelProperties.provider();
        if (provider == null || provider.isBlank() || provider.equalsIgnoreCase("ollama")) {
            return new OllamaChatModelProvider(ollamaClient);
        }
        throw new IllegalStateException("Unsupported model provider: " + provider + ". Only 'ollama' is currently implemented.");
    }
}
