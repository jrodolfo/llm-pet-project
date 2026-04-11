package net.jrodolfo.llm.model;

import net.jrodolfo.llm.dto.ChatToolMetadata;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record ChatSession(
        String sessionId,
        String model,
        Instant createdAt,
        Instant updatedAt,
        List<ChatSessionMessage> messages,
        PendingToolCall pendingToolCall
) {
    public ChatSession {
        messages = messages == null ? new ArrayList<>() : new ArrayList<>(messages);
    }

    public ChatSession withUpdatedModel(String resolvedModel) {
        return new ChatSession(sessionId, resolvedModel, createdAt, updatedAt, messages, pendingToolCall);
    }

    public ChatSession appendMessage(String role, String content, ChatToolMetadata toolMetadata, Instant timestamp) {
        List<ChatSessionMessage> updatedMessages = new ArrayList<>(messages);
        updatedMessages.add(new ChatSessionMessage(role, content, toolMetadata, timestamp));
        return new ChatSession(sessionId, model, createdAt, timestamp, updatedMessages, pendingToolCall);
    }

    public ChatSession withPendingToolCall(PendingToolCall pendingToolCall) {
        return new ChatSession(sessionId, model, createdAt, updatedAt, messages, pendingToolCall);
    }

    public static ChatSession create(String sessionId, String model, Instant now) {
        return new ChatSession(sessionId, model, now, now, List.of(), null);
    }
}
