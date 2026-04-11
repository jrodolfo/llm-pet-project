package net.jrodolfo.llm.service;

import net.jrodolfo.llm.dto.ChatSessionDetailResponse;
import net.jrodolfo.llm.dto.ChatSessionMessageResponse;
import net.jrodolfo.llm.dto.ChatSessionSummaryResponse;
import net.jrodolfo.llm.dto.PendingToolCallResponse;
import net.jrodolfo.llm.model.ChatSession;
import net.jrodolfo.llm.model.ChatSessionMessage;
import net.jrodolfo.llm.model.PendingToolCall;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ChatSessionService {

    private final FileChatSessionStore sessionStore;
    private final ChatSessionMetadataService chatSessionMetadataService;

    public ChatSessionService(FileChatSessionStore sessionStore, ChatSessionMetadataService chatSessionMetadataService) {
        this.sessionStore = sessionStore;
        this.chatSessionMetadataService = chatSessionMetadataService;
    }

    public List<ChatSessionSummaryResponse> listSessions() {
        return sessionStore.findAll().stream()
                .sorted(Comparator.comparing(ChatSession::updatedAt).reversed())
                .map(this::toSummary)
                .toList();
    }

    public ChatSessionDetailResponse getSession(String sessionId) {
        ChatSession session = sessionStore.findById(sessionId)
                .orElseThrow(() -> new ChatSessionNotFoundException(sessionId));
        return toDetail(session);
    }

    public void deleteSession(String sessionId) {
        if (!sessionStore.deleteById(sessionId)) {
            throw new ChatSessionNotFoundException(sessionId);
        }
    }

    private ChatSessionSummaryResponse toSummary(ChatSession session) {
        return new ChatSessionSummaryResponse(
                session.sessionId(),
                chatSessionMetadataService.fallbackTitle(session),
                chatSessionMetadataService.fallbackSummary(session),
                session.model(),
                session.createdAt(),
                session.updatedAt(),
                session.messages().size()
        );
    }

    private ChatSessionDetailResponse toDetail(ChatSession session) {
        return new ChatSessionDetailResponse(
                session.sessionId(),
                chatSessionMetadataService.fallbackTitle(session),
                chatSessionMetadataService.fallbackSummary(session),
                session.model(),
                session.createdAt(),
                session.updatedAt(),
                session.messages().stream().map(this::toMessageResponse).toList(),
                toPendingToolResponse(session.pendingToolCall())
        );
    }

    private ChatSessionMessageResponse toMessageResponse(ChatSessionMessage message) {
        return new ChatSessionMessageResponse(
                message.role(),
                message.content(),
                message.tool(),
                message.timestamp()
        );
    }

    public PendingToolCallResponse toPendingToolResponse(PendingToolCall pendingToolCall) {
        if (pendingToolCall == null) {
            return null;
        }

        return new PendingToolCallResponse(
                toolNameForDecision(pendingToolCall.type()),
                pendingToolCall.reason(),
                pendingToolCall.missingFields()
        );
    }

    private String toolNameForDecision(ChatToolRouterService.DecisionType type) {
        return switch (type) {
            case LIST_REPORTS -> "list_recent_reports";
            case READ_LATEST_REPORT -> "read_report_summary";
            case AWS_REGION_AUDIT -> "aws_region_audit";
            case S3_CLOUDWATCH_REPORT -> "s3_cloudwatch_report";
            case NONE -> "none";
        };
    }
}
