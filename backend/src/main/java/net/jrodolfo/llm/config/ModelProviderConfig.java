package net.jrodolfo.llm.config;

import net.jrodolfo.llm.client.BedrockRuntimeGateway;
import net.jrodolfo.llm.client.AwsSdkBedrockRuntimeGateway;
import net.jrodolfo.llm.client.BedrockCatalogClient;
import net.jrodolfo.llm.client.AwsSdkBedrockCatalogClient;
import net.jrodolfo.llm.client.OllamaClient;
import net.jrodolfo.llm.provider.BedrockChatModelProvider;
import net.jrodolfo.llm.provider.ChatModelProvider;
import net.jrodolfo.llm.provider.ChatModelProviderRegistry;
import net.jrodolfo.llm.provider.OllamaChatModelProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@Configuration
public class ModelProviderConfig {

    @Bean
    public ChatModelProvider ollamaChatModelProvider(OllamaClient ollamaClient) {
        return new OllamaChatModelProvider(ollamaClient);
    }

    @Bean
    @ConditionalOnProperty(prefix = "bedrock", name = "region")
    public ChatModelProvider bedrockChatModelProvider(
            BedrockProperties bedrockProperties,
            BedrockRuntimeGateway bedrockRuntimeGateway
    ) {
        return new BedrockChatModelProvider(bedrockRuntimeGateway, bedrockProperties);
    }

    @Bean
    public ChatModelProviderRegistry chatModelProviderRegistry(
            AppModelProperties appModelProperties,
            ChatModelProvider ollamaChatModelProvider,
            @org.springframework.beans.factory.annotation.Qualifier("bedrockChatModelProvider")
            org.springframework.beans.factory.ObjectProvider<ChatModelProvider> bedrockChatModelProviderProvider
    ) {
        Map<String, ChatModelProvider> providers = new LinkedHashMap<>();
        providers.put("ollama", ollamaChatModelProvider);
        ChatModelProvider bedrockChatModelProvider = bedrockChatModelProviderProvider.getIfAvailable();
        if (bedrockChatModelProvider != null) {
            providers.put("bedrock", bedrockChatModelProvider);
        }
        return new ChatModelProviderRegistry(appModelProperties, providers);
    }

    @Bean
    @ConditionalOnProperty(prefix = "bedrock", name = "region")
    public BedrockRuntimeGateway bedrockRuntimeGateway(BedrockProperties bedrockProperties) {
        Region bedrockRegion = Region.of(bedrockProperties.region());
        return new AwsSdkBedrockRuntimeGateway(
                BedrockRuntimeClient.builder()
                        .region(bedrockRegion)
                        .build(),
                BedrockRuntimeAsyncClient.builder()
                        .region(bedrockRegion)
                        .build()
        );
    }

    @Bean
    @ConditionalOnProperty(prefix = "bedrock", name = "region")
    public BedrockCatalogClient bedrockCatalogClient(BedrockProperties bedrockProperties) {
        Region bedrockRegion = Region.of(bedrockProperties.region());
        return new AwsSdkBedrockCatalogClient(
                BedrockClient.builder()
                        .region(bedrockRegion)
                        .build()
        );
    }
}
