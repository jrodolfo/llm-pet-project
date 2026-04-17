package net.jrodolfo.llm.provider;

import net.jrodolfo.llm.config.AppModelProperties;
import net.jrodolfo.llm.service.InvalidProviderException;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatModelProviderRegistry {

    private final String defaultProvider;
    private final Map<String, ChatModelProvider> providers;

    public ChatModelProviderRegistry(AppModelProperties appModelProperties, Map<String, ChatModelProvider> providers) {
        this.providers = Map.copyOf(providers);
        this.defaultProvider = normalizeProvider(appModelProperties.provider());
        if (!this.providers.containsKey(this.defaultProvider)) {
            throw new IllegalStateException(
                    "Configured default provider '%s' is not available. Supported providers are: %s"
                            .formatted(this.defaultProvider, this.providers.keySet())
            );
        }
    }

    public ChatModelProvider get(String provider) {
        String resolvedProvider = provider == null || provider.isBlank() ? defaultProvider : normalizeProvider(provider);
        ChatModelProvider chatModelProvider = providers.get(resolvedProvider);
        if (chatModelProvider == null) {
            throw new InvalidProviderException(
                    "Unsupported model provider: %s. Supported providers are: %s"
                            .formatted(resolvedProvider, supportedProviders())
            );
        }
        return chatModelProvider;
    }

    public String resolveProviderName(String provider) {
        return provider == null || provider.isBlank() ? defaultProvider : normalizeProvider(provider);
    }

    public String defaultProvider() {
        return defaultProvider;
    }

    public List<String> supportedProviders() {
        return providers.keySet().stream().sorted().toList();
    }

    private String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return "ollama";
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }
}
