package net.jrodolfo.llm.health;

import net.jrodolfo.llm.config.McpProperties;
import net.jrodolfo.llm.client.McpClient;
import net.jrodolfo.llm.client.McpClientException;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component("mcp")
public class McpHealthIndicator implements HealthIndicator {

    private final McpProperties mcpProperties;
    private final McpClient mcpClient;

    public McpHealthIndicator(McpProperties mcpProperties, McpClient mcpClient) {
        this.mcpProperties = mcpProperties;
        this.mcpClient = mcpClient;
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

        try {
            List<McpClient.McpToolDescriptor> tools = mcpClient.listTools();
            return Health.up()
                    .withDetail("enabled", true)
                    .withDetail("status", "ready")
                    .withDetail("command", mcpProperties.command())
                    .withDetail("workingDirectory", workingDirectory.toString())
                    .withDetail("workingDirectoryExists", true)
                    .withDetail("entrypoint", entrypointPath.toString())
                    .withDetail("entrypointExists", true)
                    .withDetail("toolTimeoutSeconds", mcpProperties.toolTimeoutSeconds())
                    .withDetail("startupTimeoutSeconds", mcpProperties.startupTimeoutSeconds())
                    .withDetail("runnable", true)
                    .withDetail("toolCount", tools.size())
                    .build();
        } catch (McpClientException ex) {
            return Health.down()
                    .withDetail("enabled", true)
                    .withDetail("status", "unrunnable")
                    .withDetail("command", mcpProperties.command())
                    .withDetail("workingDirectory", workingDirectory.toString())
                    .withDetail("workingDirectoryExists", true)
                    .withDetail("entrypoint", entrypointPath.toString())
                    .withDetail("entrypointExists", true)
                    .withDetail("runnable", false)
                    .withDetail("error", ex.getMessage())
                    .build();
        }
    }
}
