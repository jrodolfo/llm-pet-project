package net.jrodolfo.llm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Compact provider readiness and troubleshooting status for the chat UI.")
public record ProviderStatusResponse(
        @Schema(description = "Provider whose status is being reported.", example = "ollama")
        String provider,
        @Schema(description = "Normalized status for the selected provider.", example = "ready")
        String status,
        @Schema(description = "Short user-facing explanation of the current provider state.")
        String message
) {
}
