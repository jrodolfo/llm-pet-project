package net.jrodolfo.llm.health;

import net.jrodolfo.llm.config.McpProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component("mcp")
public class McpHealthIndicator implements HealthIndicator {

    private final McpProperties mcpProperties;

    public McpHealthIndicator(McpProperties mcpProperties) {
        this.mcpProperties = mcpProperties;
    }

    @Override
    public Health health() {
        boolean commandConfigured = mcpProperties.command() != null && !mcpProperties.command().isBlank();
        Path workingDirectory = mcpProperties.resolvedWorkingDirectory();
        boolean workingDirectoryExists = Files.isDirectory(workingDirectory);

        if (!mcpProperties.enabled()) {
            return Health.up()
                    .withDetail("enabled", false)
                    .withDetail("status", "disabled")
                    .withDetail("command", mcpProperties.command())
                    .withDetail("commandConfigured", commandConfigured)
                    .withDetail("workingDirectory", workingDirectory.toString())
                    .withDetail("workingDirectoryExists", workingDirectoryExists)
                    .build();
        }

        if (!commandConfigured || !workingDirectoryExists) {
            return Health.down()
                    .withDetail("enabled", true)
                    .withDetail("status", "misconfigured")
                    .withDetail("command", mcpProperties.command())
                    .withDetail("commandConfigured", commandConfigured)
                    .withDetail("workingDirectory", workingDirectory.toString())
                    .withDetail("workingDirectoryExists", workingDirectoryExists)
                    .build();
        }

        return Health.up()
                .withDetail("enabled", true)
                .withDetail("status", "enabled")
                .withDetail("command", mcpProperties.command())
                .withDetail("workingDirectory", workingDirectory.toString())
                .withDetail("workingDirectoryExists", true)
                .withDetail("toolTimeoutSeconds", mcpProperties.toolTimeoutSeconds())
                .withDetail("startupTimeoutSeconds", mcpProperties.startupTimeoutSeconds())
                .build();
    }
}
