package net.jrodolfo.llm.health;

import net.jrodolfo.llm.config.McpProperties;
import net.jrodolfo.llm.client.McpClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component("mcp")
public class McpHealthIndicator implements HealthIndicator {

    private final McpProperties mcpProperties;

    public McpHealthIndicator(McpProperties mcpProperties, McpClient mcpClient) {
        this.mcpProperties = mcpProperties;
    }

    @Override
    public Health health() {
        boolean commandConfigured = mcpProperties.command() != null && !mcpProperties.command().isBlank();
        Path workingDirectory = mcpProperties.resolvedWorkingDirectory();
        boolean workingDirectoryExists = Files.isDirectory(workingDirectory);
        String entrypoint = mcpProperties.args() == null || mcpProperties.args().isEmpty() ? "" : mcpProperties.args().getFirst();
        Path entrypointPath = entrypoint.isBlank() ? workingDirectory : workingDirectory.resolve(entrypoint).normalize();
        boolean entrypointExists = Files.exists(entrypointPath);

        if (!mcpProperties.enabled()) {
            return Health.up()
                    .withDetail("enabled", false)
                    .withDetail("status", "disabled")
                    .withDetail("command", mcpProperties.command())
                    .withDetail("commandConfigured", commandConfigured)
                    .withDetail("workingDirectory", workingDirectory.toString())
                    .withDetail("workingDirectoryExists", workingDirectoryExists)
                    .withDetail("entrypoint", entrypointPath.toString())
                    .withDetail("entrypointExists", entrypointExists)
                    .build();
        }

        if (!commandConfigured || !workingDirectoryExists || !entrypointExists) {
            return Health.down()
                    .withDetail("enabled", true)
                    .withDetail("status", "misconfigured")
                    .withDetail("command", mcpProperties.command())
                    .withDetail("commandConfigured", commandConfigured)
                    .withDetail("workingDirectory", workingDirectory.toString())
                    .withDetail("workingDirectoryExists", workingDirectoryExists)
                    .withDetail("entrypoint", entrypointPath.toString())
                    .withDetail("entrypointExists", entrypointExists)
                    .build();
        }

        return Health.up()
                .withDetail("enabled", true)
                .withDetail("status", "ready")
                .withDetail("command", mcpProperties.command())
                .withDetail("commandConfigured", true)
                .withDetail("workingDirectory", workingDirectory.toString())
                .withDetail("workingDirectoryExists", true)
                .withDetail("entrypoint", entrypointPath.toString())
                .withDetail("entrypointExists", true)
                .withDetail("toolTimeoutSeconds", mcpProperties.toolTimeoutSeconds())
                .withDetail("startupTimeoutSeconds", mcpProperties.startupTimeoutSeconds())
                .withDetail("runnable", true)
                .withDetail("probe", "config-only")
                .build();
    }
}
