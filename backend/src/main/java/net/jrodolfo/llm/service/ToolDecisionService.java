package net.jrodolfo.llm.service;

import net.jrodolfo.llm.config.AppToolsProperties;
import net.jrodolfo.llm.model.PendingToolCall;
import org.springframework.stereotype.Service;

@Service
public class ToolDecisionService {

    private final AppToolsProperties appToolsProperties;
    private final LlmToolPlannerService llmToolPlannerService;
    private final ChatToolRouterService ruleBasedRouter;

    public ToolDecisionService(
            AppToolsProperties appToolsProperties,
            LlmToolPlannerService llmToolPlannerService,
            ChatToolRouterService ruleBasedRouter
    ) {
        this.appToolsProperties = appToolsProperties;
        this.llmToolPlannerService = llmToolPlannerService;
        this.ruleBasedRouter = ruleBasedRouter;
    }

    public ChatToolRouterService.ToolDecision route(String message, String model) {
        return switch (routingMode()) {
            case "rules" -> ruleBasedRouter.route(message);
            case "llm" -> llmToolPlannerService.plan(message, model).orElse(ChatToolRouterService.ToolDecision.none());
            default -> llmToolPlannerService.plan(message, model).orElseGet(() -> ruleBasedRouter.route(message));
        };
    }

    public ChatToolRouterService.ToolDecision resolvePending(PendingToolCall pendingToolCall, String message, String model) {
        return switch (routingMode()) {
            case "rules" -> ruleBasedRouter.resolvePending(pendingToolCall, message);
            case "llm" -> llmToolPlannerService.resolvePending(pendingToolCall, message, model)
                    .orElse(ChatToolRouterService.ToolDecision.none());
            default -> llmToolPlannerService.resolvePending(pendingToolCall, message, model)
                    .orElseGet(() -> ruleBasedRouter.resolvePending(pendingToolCall, message));
        };
    }

    private String routingMode() {
        String configured = appToolsProperties.routingMode();
        if (configured == null || configured.isBlank()) {
            return "hybrid";
        }
        return configured.trim().toLowerCase();
    }
}
