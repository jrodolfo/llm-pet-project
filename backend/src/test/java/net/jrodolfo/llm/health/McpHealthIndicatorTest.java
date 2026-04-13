package net.jrodolfo.llm.health;

import net.jrodolfo.llm.client.McpClient;
import net.jrodolfo.llm.config.McpProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.actuate.health.Status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpHealthIndicatorTest {

    @TempDir
    Path tempDir;

    @Test
    void mcpHealthIsDownWhenEntrypointIsMissing() {
        McpProperties properties = new McpProperties(true, "node", List.of("dist/index.js"), tempDir.toString(), 10, 120);
        McpHealthIndicator indicator = new McpHealthIndicator(properties, null);

        var health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("misconfigured", health.getDetails().get("status"));
    }

    @Test
    void mcpHealthIsUpWhenConfigurationIsRunnable() throws Exception {
        Path distDir = Files.createDirectories(tempDir.resolve("dist"));
        Files.writeString(distDir.resolve("index.js"), "console.log('ok');");

        McpProperties properties = new McpProperties(true, "node", List.of("dist/index.js"), tempDir.toString(), 10, 120);
        McpHealthIndicator indicator = new McpHealthIndicator(properties, null);

        var health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("ready", health.getDetails().get("status"));
        assertEquals("config-only", health.getDetails().get("probe"));
    }

    @Test
    void mcpHealthIsUpWhenDisabled() {
        McpProperties properties = new McpProperties(false, "node", List.of("dist/index.js"), tempDir.toString(), 10, 120);
        McpHealthIndicator indicator = new McpHealthIndicator(properties, null);

        var health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("disabled", health.getDetails().get("status"));
    }
}
