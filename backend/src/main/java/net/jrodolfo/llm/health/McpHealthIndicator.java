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
        if (!mcpProperties.enabled()) {
            return Health.up()
                    .withDetail("enabled", false)
                    .withDetail("status", "disabled")
                    .build();
        }

        boolean commandConfigured = mcpProperties.command() != null && !mcpProperties.command().isBlank();
        Path workingDirectory = mcpProperties.workingDirectory() == null || mcpProperties.workingDirectory().isBlank()
                ? null
                : Path.of(mcpProperties.workingDirectory()).toAbsolutePath().normalize();
        boolean workingDirectoryExists = workingDirectory != null && Files.isDirectory(workingDirectory);

        if (!commandConfigured || !workingDirectoryExists) {
            return Health.down()
                    .withDetail("enabled", true)
                    .withDetail("commandConfigured", commandConfigured)
                    .withDetail("workingDirectory", workingDirectory == null ? "" : workingDirectory.toString())
                    .withDetail("workingDirectoryExists", workingDirectoryExists)
                    .build();
        }

        return Health.up()
                .withDetail("enabled", true)
                .withDetail("command", mcpProperties.command())
                .withDetail("workingDirectory", workingDirectory.toString())
                .withDetail("toolTimeoutSeconds", mcpProperties.toolTimeoutSeconds())
                .build();
    }
}
