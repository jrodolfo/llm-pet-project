package net.jrodolfo.llm.health;

import net.jrodolfo.llm.config.AppModelProperties;
import net.jrodolfo.llm.config.BedrockProperties;
import net.jrodolfo.llm.config.OllamaProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

import java.io.UncheckedIOException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component("modelProvider")
public class ModelProviderHealthIndicator implements HealthIndicator {

    private final AppModelProperties appModelProperties;
    private final OllamaProperties ollamaProperties;
    private final BedrockProperties bedrockProperties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Supplier<Boolean> bedrockCredentialsResolver;

    @Autowired
    public ModelProviderHealthIndicator(
            AppModelProperties appModelProperties,
            OllamaProperties ollamaProperties,
            BedrockProperties bedrockProperties,
            ObjectProvider<ObjectMapper> objectMapperProvider
    ) {
        this(
                appModelProperties,
                ollamaProperties,
                bedrockProperties,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(Math.max(1, ollamaProperties.connectTimeoutSeconds())))
                        .build(),
                objectMapperProvider.getIfAvailable(ObjectMapper::new),
                () -> {
                    AwsCredentialsProvider provider = DefaultCredentialsProvider.create();
                    provider.resolveCredentials();
                    return true;
                }
        );
    }

    ModelProviderHealthIndicator(
            AppModelProperties appModelProperties,
            OllamaProperties ollamaProperties,
            BedrockProperties bedrockProperties,
            HttpClient httpClient,
            ObjectMapper objectMapper,
            Supplier<Boolean> bedrockCredentialsResolver
    ) {
        this.appModelProperties = appModelProperties;
        this.ollamaProperties = ollamaProperties;
        this.bedrockProperties = bedrockProperties;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.bedrockCredentialsResolver = bedrockCredentialsResolver;
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
        boolean credentialsResolved = false;

        if (regionConfigured && modelConfigured) {
            try {
                credentialsResolved = Boolean.TRUE.equals(bedrockCredentialsResolver.get());
            } catch (RuntimeException ignored) {
                credentialsResolved = false;
            }
        }

        if (!regionConfigured || !modelConfigured || !credentialsResolved) {
            return Health.down()
                    .withDetail("provider", "bedrock")
                    .withDetail("regionConfigured", regionConfigured)
                    .withDetail("modelConfigured", modelConfigured)
                    .withDetail("credentialsResolved", credentialsResolved)
                    .withDetail("status", "misconfigured")
                    .withDetail("error", "BEDROCK_REGION, BEDROCK_MODEL_ID, and AWS credentials must all be configured.")
                    .build();
        }

        return Health.up()
                .withDetail("provider", "bedrock")
                .withDetail("status", "ready")
                .withDetail("region", bedrockProperties.region())
                .withDetail("modelId", bedrockProperties.modelId())
                .withDetail("credentialsResolved", true)
                .build();
    }

    private Health ollamaHealth() {
        String baseUrl = ollamaProperties.baseUrl();
        String defaultModel = ollamaProperties.defaultModel();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(Math.max(1, ollamaProperties.connectTimeoutSeconds())))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                boolean modelPresent = ollamaModelPresent(response.body(), defaultModel);
                if (!modelPresent) {
                    return Health.status(Status.DOWN)
                            .withDetail("provider", "ollama")
                            .withDetail("status", "reachable")
                            .withDetail("baseUrl", baseUrl)
                            .withDetail("defaultModel", defaultModel)
                            .withDetail("reachable", true)
                            .withDetail("modelPresent", false)
                            .withDetail("error", "Configured default model is not present in Ollama.")
                            .build();
                }
                return Health.up()
                        .withDetail("provider", "ollama")
                        .withDetail("status", "ready")
                        .withDetail("baseUrl", baseUrl)
                        .withDetail("defaultModel", defaultModel)
                        .withDetail("reachable", true)
                        .withDetail("modelPresent", true)
                        .build();
            }
            return Health.down()
                    .withDetail("provider", "ollama")
                    .withDetail("status", "unreachable")
                    .withDetail("baseUrl", baseUrl)
                    .withDetail("defaultModel", defaultModel)
                    .withDetail("statusCode", response.statusCode())
                    .withDetail("error", "Ollama tags endpoint returned a non-success status.")
                    .build();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Health.down()
                    .withDetail("provider", "ollama")
                    .withDetail("status", "interrupted")
                    .withDetail("baseUrl", baseUrl)
                    .withDetail("error", "Ollama reachability check was interrupted.")
                    .build();
        } catch (IOException | IllegalArgumentException ex) {
            return Health.down()
                    .withDetail("provider", "ollama")
                    .withDetail("status", "unreachable")
                    .withDetail("baseUrl", baseUrl)
                    .withDetail("defaultModel", defaultModel)
                    .withDetail("error", ex.getMessage())
                    .build();
        }
    }

    private boolean ollamaModelPresent(String responseBody, String defaultModel) {
        if (defaultModel == null || defaultModel.isBlank()) {
            return true;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode models = root.path("models");
            if (!models.isArray()) {
                return false;
            }
            for (JsonNode model : models) {
                String name = model.path("name").asText();
                if (defaultModel.equals(name)) {
                    return true;
                }
            }
            return false;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return "ollama";
        }
        return provider.trim().toLowerCase();
    }
}
