package net.jrodolfo.llm.model;

import net.jrodolfo.llm.service.ChatToolRouterService;

import java.util.List;

public record PendingToolCall(
        ChatToolRouterService.DecisionType type,
        String reportType,
        String bucket,
        String region,
        Integer days,
        String reason,
        List<String> services,
        List<String> missingFields
) {
    public PendingToolCall {
        services = services == null ? List.of() : List.copyOf(services);
        missingFields = missingFields == null ? List.of() : List.copyOf(missingFields);
    }
}
