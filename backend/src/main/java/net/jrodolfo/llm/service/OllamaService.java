package net.jrodolfo.llm.service;

import net.jrodolfo.llm.client.OllamaClient;
import net.jrodolfo.llm.dto.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class OllamaService {

    private final OllamaClient ollamaClient;

    public OllamaService(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    public ChatResponse chat(String message, String model) {
        String normalizedMessage = message.trim();
        String resolvedModel = ollamaClient.resolveModel(model);
        String response = ollamaClient.generate(normalizedMessage, resolvedModel);
        return new ChatResponse(response, resolvedModel);
    }

    public void streamChat(String message, String model, Consumer<String> tokenConsumer) {
        String normalizedMessage = message.trim();
        String resolvedModel = ollamaClient.resolveModel(model);
        ollamaClient.streamGenerate(normalizedMessage, resolvedModel, tokenConsumer);
    }
}
