package net.jrodolfo.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tools")
public record AppToolsProperties(
        String routingMode
) {
}
