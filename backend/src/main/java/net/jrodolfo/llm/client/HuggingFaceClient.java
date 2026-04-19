package net.jrodolfo.llm.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jrodolfo.llm.config.HuggingFaceProperties;
import net.jrodolfo.llm.dto.ModelProviderMetadata;
import net.jrodolfo.llm.provider.ProviderPromptMessage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Minimal client for Hugging Face's hosted chat-completions endpoint.
 */
public class HuggingFaceClient {

    private final ObjectMapper objectMapper;
    private final HuggingFaceProperties huggingFaceProperties;
    private final HttpClient httpClient;

    public HuggingFaceClient(ObjectMapper objectMapper, HuggingFaceProperties huggingFaceProperties) {
        this(
                objectMapper,
                huggingFaceProperties,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(Math.max(1, huggingFaceProperties.connectTimeoutSeconds())))
                        .build()
        );
    }

    HuggingFaceClient(
            ObjectMapper objectMapper,
            HuggingFaceProperties huggingFaceProperties,
            HttpClient httpClient
    ) {
        this.objectMapper = objectMapper;
        this.huggingFaceProperties = huggingFaceProperties;
        this.httpClient = httpClient;
    }

    public ModelProviderReply chat(List<ProviderPromptMessage> messages, String model) {
        long startedAt = System.nanoTime();
        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "messages", messages.stream()
                            .map(message -> Map.of("role", message.role(), "content", message.content()))
                            .toList(),
                    "stream", false
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(huggingFaceProperties.baseUrl()))
                    .timeout(Duration.ofSeconds(Math.max(1, huggingFaceProperties.readTimeoutSeconds())))
                    .header("Authorization", "Bearer " + huggingFaceProperties.apiToken().trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ModelProviderException(parseErrorMessage(response.body(), response.statusCode()));
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode usage = root.path("usage");
            return new ModelProviderReply(
                    parseResponseText(root),
                    new ModelProviderMetadata(
                            "huggingface",
                            model,
                            textValue(root.path("choices").path(0).path("finish_reason")),
                            intValue(usage.path("prompt_tokens")),
                            intValue(usage.path("completion_tokens")),
                            intValue(usage.path("total_tokens")),
                            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt),
                            null,
                            null,
                            null
                    )
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ModelProviderException("Hugging Face request was interrupted.", ex);
        } catch (IOException ex) {
            throw new ModelProviderException("Hugging Face request failed.", ex);
        }
    }

    private String parseResponseText(JsonNode root) {
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode part : contentNode) {
                JsonNode text = part.path("text");
                if (text.isTextual()) {
                    builder.append(text.asText());
                }
            }
            String combined = builder.toString();
            if (!combined.isBlank()) {
                return combined;
            }
        }
        throw new ModelProviderException("Hugging Face response did not contain chat content.");
    }

    private String parseErrorMessage(String responseBody, int statusCode) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode errorNode = root.path("error");
            if (errorNode.isTextual() && !errorNode.asText().isBlank()) {
                return "Hugging Face request failed: " + errorNode.asText();
            }
            JsonNode messageNode = errorNode.path("message");
            if (messageNode.isTextual() && !messageNode.asText().isBlank()) {
                return "Hugging Face request failed: " + messageNode.asText();
            }
        } catch (IOException ignored) {
            // Fall through to the generic error below.
        }
        return "Hugging Face request failed with status " + statusCode + ".";
    }

    private Integer intValue(JsonNode node) {
        return node.isNumber() ? node.asInt() : null;
    }

    private String textValue(JsonNode node) {
        if (!node.isTextual()) {
            return null;
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : value;
    }
}
