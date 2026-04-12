package net.jrodolfo.llm.service;

import net.jrodolfo.llm.dto.ChatToolMetadata;
import net.jrodolfo.llm.dto.ModelProviderMetadata;
import net.jrodolfo.llm.model.ChatSession;
import net.jrodolfo.llm.model.ChatSessionMessage;
import net.jrodolfo.llm.model.PendingToolCall;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ChatMemoryService {

    private final FileChatSessionStore sessionStore;
    private final ChatSessionMetadataService chatSessionMetadataService;

    public ChatMemoryService(FileChatSessionStore sessionStore, ChatSessionMetadataService chatSessionMetadataService) {
        this.sessionStore = sessionStore;
        this.chatSessionMetadataService = chatSessionMetadataService;
    }

    public ChatSession startTurn(String requestedSessionId, String requestedModel, String resolvedModel, String userMessage) {
        Instant now = Instant.now();
        String sessionId = requestedSessionId == null || requestedSessionId.isBlank()
                ? UUID.randomUUID().toString()
                : requestedSessionId.trim();

        ChatSession existingSession = sessionStore.findById(sessionId)
                .map(session -> session.withUpdatedModel(resolvedModel))
                .orElseGet(() -> ChatSession.create(sessionId, resolvedModel, now));

        return existingSession.appendMessage("user", userMessage.trim(), null, null, now);
    }

    public ChatSession finishTurn(ChatSession session, String assistantMessage, ChatToolMetadata toolMetadata) {
        return finishTurn(session, assistantMessage, toolMetadata, null, session.pendingToolCall());
    }

    public ChatSession finishTurn(
            ChatSession session,
            String assistantMessage,
            ChatToolMetadata toolMetadata,
            ModelProviderMetadata providerMetadata,
            PendingToolCall pendingToolCall
    ) {
        ChatSession updatedSession = session
                .withPendingToolCall(pendingToolCall)
                .appendMessage("assistant", assistantMessage, toolMetadata, providerMetadata, Instant.now());
        return sessionStore.save(chatSessionMetadataService.enrich(updatedSession));
    }

    public List<ChatSessionMessage> historyBeforeLatestUserMessage(ChatSession session) {
        List<ChatSessionMessage> messages = session.messages();
        if (messages.isEmpty()) {
            return List.of();
        }
        return messages.subList(0, messages.size() - 1);
    }
}
