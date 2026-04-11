package net.jrodolfo.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.model")
public record AppModelProperties(
        String provider
) {
}
