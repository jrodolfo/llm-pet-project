package net.jrodolfo.llm.health;

import net.jrodolfo.llm.client.McpClient;
import net.jrodolfo.llm.client.McpClientException;
import net.jrodolfo.llm.config.McpProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.actuate.health.Status;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpHealthIndicatorTest {

    @TempDir
    Path tempDir;

    @Test
    void mcpHealthIsDownWhenEntrypointIsMissing() {
        McpProperties properties = new McpProperties(true, "node", List.of("dist/index.js"), tempDir.toString(), 10, 120);
        McpHealthIndicator indicator = new McpHealthIndicator(properties, new FakeMcpClient(properties, List.of(), null));

        var health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("misconfigured", health.getDetails().get("status"));
    }

    @Test
    void mcpHealthIsUpWhenToolsCanBeListed() throws Exception {
        Path distDir = Files.createDirectories(tempDir.resolve("dist"));
        Files.writeString(distDir.resolve("index.js"), "console.log('ok');");

        McpClient client = new FakeMcpClient(
                new McpProperties(true, "node", List.of("dist/index.js"), tempDir.toString(), 10, 120),
                List.of(new McpClient.McpToolDescriptor("aws_region_audit", "AWS Region Audit", "desc", Map.of())),
                null
        );

        McpProperties properties = new McpProperties(true, "node", List.of("dist/index.js"), tempDir.toString(), 10, 120);
        McpHealthIndicator indicator = new McpHealthIndicator(properties, client);

        var health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("ready", health.getDetails().get("status"));
        assertEquals(1, health.getDetails().get("toolCount"));
    }

    @Test
    void mcpHealthIsDownWhenListToolsFails() throws Exception {
        Path distDir = Files.createDirectories(tempDir.resolve("dist"));
        Files.writeString(distDir.resolve("index.js"), "console.log('ok');");

        McpClient client = new FakeMcpClient(
                new McpProperties(true, "node", List.of("dist/index.js"), tempDir.toString(), 10, 120),
                List.of(),
                new McpClientException("handshake failed")
        );

        McpProperties properties = new McpProperties(true, "node", List.of("dist/index.js"), tempDir.toString(), 10, 120);
        McpHealthIndicator indicator = new McpHealthIndicator(properties, client);

        var health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("unrunnable", health.getDetails().get("status"));
    }

    private static final class FakeMcpClient extends McpClient {
        private final List<McpToolDescriptor> tools;
        private final McpClientException listToolsException;

        private FakeMcpClient(McpProperties properties, List<McpToolDescriptor> tools, McpClientException listToolsException) {
            super(new ObjectMapper(), properties);
            this.tools = tools;
            this.listToolsException = listToolsException;
        }

        @Override
        public List<McpToolDescriptor> listTools() {
            if (listToolsException != null) {
                throw listToolsException;
            }
            return tools;
        }
    }
}
