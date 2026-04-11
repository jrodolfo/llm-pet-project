package net.jrodolfo.llm.service;

import net.jrodolfo.llm.model.ChatSession;
import net.jrodolfo.llm.model.ChatSessionMessage;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatSessionMetadataService {

    private static final int TITLE_MAX_LENGTH = 60;
    private static final int SUMMARY_MAX_LENGTH = 120;

    public ChatSession enrich(ChatSession session) {
        String title = hasText(session.title()) ? session.title() : generateTitle(session.messages());
        String summary = hasText(session.summary()) ? session.summary() : generateSummary(session.messages(), title);
        return session.withMetadata(title, summary);
    }

    public String fallbackTitle(ChatSession session) {
        return hasText(session.title()) ? session.title() : generateTitle(session.messages());
    }

    public String fallbackSummary(ChatSession session) {
        return hasText(session.summary()) ? session.summary() : generateSummary(session.messages(), fallbackTitle(session));
    }

    private String generateTitle(List<ChatSessionMessage> messages) {
        return messages.stream()
                .filter(message -> "user".equals(message.role()))
                .map(ChatSessionMessage::content)
                .map(this::normalizeTitle)
                .filter(this::hasText)
                .findFirst()
                .orElse("New chat");
    }

    private String generateSummary(List<ChatSessionMessage> messages, String title) {
        if (messages == null || messages.isEmpty()) {
            return title;
        }

        String latestAssistant = messages.stream()
                .filter(message -> "assistant".equals(message.role()))
                .reduce((first, second) -> second)
                .map(ChatSessionMessage::content)
                .map(this::normalizeSummary)
                .filter(this::hasText)
                .orElse(null);

        if (latestAssistant != null) {
            return latestAssistant;
        }

        return title;
    }

    private String normalizeTitle(String content) {
        String normalized = normalizeWhitespace(content);
        if (!hasText(normalized)) {
            return "New chat";
        }
        if (normalized.length() <= TITLE_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, TITLE_MAX_LENGTH - 1) + "…";
    }

    private String normalizeSummary(String content) {
        String normalized = normalizeWhitespace(content);
        if (!hasText(normalized)) {
            return "";
        }
        if (normalized.length() <= SUMMARY_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, SUMMARY_MAX_LENGTH - 1) + "…";
    }

    private String normalizeWhitespace(String content) {
        return content == null ? "" : content.trim().replaceAll("\\s+", " ");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
