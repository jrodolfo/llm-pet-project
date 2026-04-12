package net.jrodolfo.llm.health;

import net.jrodolfo.llm.config.AppModelProperties;
import net.jrodolfo.llm.config.BedrockProperties;
import net.jrodolfo.llm.config.OllamaProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component("modelProvider")
public class ModelProviderHealthIndicator implements HealthIndicator {

    private final AppModelProperties appModelProperties;
    private final OllamaProperties ollamaProperties;
    private final BedrockProperties bedrockProperties;
    private final HttpClient httpClient;

    public ModelProviderHealthIndicator(
            AppModelProperties appModelProperties,
            OllamaProperties ollamaProperties,
            BedrockProperties bedrockProperties
    ) {
        this.appModelProperties = appModelProperties;
        this.ollamaProperties = ollamaProperties;
        this.bedrockProperties = bedrockProperties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(1, ollamaProperties.connectTimeoutSeconds())))
                .build();
    }

    @Override
    public Health health() {
        String provider = normalizeProvider(appModelProperties.provider());
        return switch (provider) {
            case "bedrock" -> bedrockHealth();
            case "ollama" -> ollamaHealth();
            default -> Health.down()
                    .withDetail("provider", provider)
                    .withDetail("error", "Unsupported model provider.")
                    .build();
        };
    }

    private Health bedrockHealth() {
        boolean regionConfigured = bedrockProperties.region() != null && !bedrockProperties.region().isBlank();
        boolean modelConfigured = bedrockProperties.modelId() != null && !bedrockProperties.modelId().isBlank();

        if (!regionConfigured || !modelConfigured) {
            return Health.down()
                    .withDetail("provider", "bedrock")
                    .withDetail("regionConfigured", regionConfigured)
                    .withDetail("modelConfigured", modelConfigured)
                    .withDetail("error", "BEDROCK_REGION and BEDROCK_MODEL_ID must both be configured.")
                    .build();
        }

        return Health.up()
                .withDetail("provider", "bedrock")
                .withDetail("region", bedrockProperties.region())
                .withDetail("modelId", bedrockProperties.modelId())
                .build();
    }

    private Health ollamaHealth() {
        String baseUrl = ollamaProperties.baseUrl();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(Math.max(1, ollamaProperties.connectTimeoutSeconds())))
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return Health.up()
                        .withDetail("provider", "ollama")
                        .withDetail("baseUrl", baseUrl)
                        .withDetail("defaultModel", ollamaProperties.defaultModel())
                        .withDetail("reachable", true)
                        .build();
            }
            return Health.down()
                    .withDetail("provider", "ollama")
                    .withDetail("baseUrl", baseUrl)
                    .withDetail("defaultModel", ollamaProperties.defaultModel())
                    .withDetail("statusCode", response.statusCode())
                    .withDetail("error", "Ollama tags endpoint returned a non-success status.")
                    .build();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Health.down()
                    .withDetail("provider", "ollama")
                    .withDetail("baseUrl", baseUrl)
                    .withDetail("error", "Ollama reachability check was interrupted.")
                    .build();
        } catch (IOException | IllegalArgumentException ex) {
            return Health.down()
                    .withDetail("provider", "ollama")
                    .withDetail("baseUrl", baseUrl)
                    .withDetail("defaultModel", ollamaProperties.defaultModel())
                    .withDetail("error", ex.getMessage())
                    .build();
        }
    }

    private String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return "ollama";
        }
        return provider.trim().toLowerCase();
    }
}
