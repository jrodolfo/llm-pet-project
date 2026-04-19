package net.jrodolfo.llm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.jrodolfo.llm.client.BedrockRuntimeGateway;
import net.jrodolfo.llm.client.HuggingFaceClient;
import net.jrodolfo.llm.client.ModelProviderReply;
import net.jrodolfo.llm.client.OllamaClient;
import net.jrodolfo.llm.dto.ModelProviderMetadata;
import net.jrodolfo.llm.provider.ChatModelProvider;
import net.jrodolfo.llm.provider.ChatModelProviderRegistry;
import net.jrodolfo.llm.provider.HuggingFaceChatModelProvider;
import net.jrodolfo.llm.provider.OllamaChatModelProvider;
import net.jrodolfo.llm.provider.ProviderPromptMessage;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelProviderConfigTest {

    private final ModelProviderConfig modelProviderConfig = new ModelProviderConfig();

    @Test
    void registryUsesConfiguredDefaultProvider() {
        ChatModelProviderRegistry registry = modelProviderConfig.chatModelProviderRegistry(
                new AppModelProperties("ollama"),
                modelProviderConfig.ollamaChatModelProvider(new OllamaClient(new ObjectMapper(), new OllamaProperties("http://localhost:11434", "llama3:8b", 10, 10))),
                new org.springframework.beans.factory.support.StaticListableBeanFactory().getBeanProvider(ChatModelProvider.class),
                new org.springframework.beans.factory.support.StaticListableBeanFactory().getBeanProvider(ChatModelProvider.class)
        );

        assertEquals("ollama", registry.defaultProvider());
        assertEquals(java.util.List.of("ollama"), registry.supportedProviders());
        assertInstanceOf(OllamaChatModelProvider.class, registry.get("ollama"));
    }

    @Test
    void registrySupportsBothProvidersWhenBedrockIsAvailable() {
        org.springframework.beans.factory.support.StaticListableBeanFactory beanFactory = new org.springframework.beans.factory.support.StaticListableBeanFactory();
        beanFactory.addBean("bedrockChatModelProvider", modelProviderConfig.bedrockChatModelProvider(
                new BedrockProperties("us-east-1", "amazon.nova-lite-v1:0"),
                new FakeBedrockRuntimeGateway()
        ));

        ChatModelProviderRegistry registry = modelProviderConfig.chatModelProviderRegistry(
                new AppModelProperties("bedrock"),
                modelProviderConfig.ollamaChatModelProvider(new OllamaClient(new ObjectMapper(), new OllamaProperties("http://localhost:11434", "llama3:8b", 10, 10))),
                beanFactory.getBeanProvider(ChatModelProvider.class),
                new org.springframework.beans.factory.support.StaticListableBeanFactory().getBeanProvider(ChatModelProvider.class)
        );

        assertEquals("bedrock", registry.defaultProvider());
        assertEquals(java.util.List.of("bedrock", "ollama"), registry.supportedProviders());
    }

    @Test
    void registrySupportsHuggingFaceWhenAvailable() {
        org.springframework.beans.factory.support.StaticListableBeanFactory beanFactory = new org.springframework.beans.factory.support.StaticListableBeanFactory();
        beanFactory.addBean("huggingFaceChatModelProvider", modelProviderConfig.huggingFaceChatModelProvider(
                new HuggingFaceClient(
                        new ObjectMapper(),
                        new HuggingFaceProperties("https://router.huggingface.co/v1/chat/completions", "token", "meta-llama/Llama-3.1-8B-Instruct", java.util.List.of("meta-llama/Llama-3.1-8B-Instruct"), 10, 60)
                ),
                new HuggingFaceProperties("https://router.huggingface.co/v1/chat/completions", "token", "meta-llama/Llama-3.1-8B-Instruct", java.util.List.of("meta-llama/Llama-3.1-8B-Instruct"), 10, 60)
        ));

        ChatModelProviderRegistry registry = modelProviderConfig.chatModelProviderRegistry(
                new AppModelProperties("huggingface"),
                modelProviderConfig.ollamaChatModelProvider(new OllamaClient(new ObjectMapper(), new OllamaProperties("http://localhost:11434", "llama3:8b", 10, 10))),
                new org.springframework.beans.factory.support.StaticListableBeanFactory().getBeanProvider(ChatModelProvider.class),
                beanFactory.getBeanProvider(ChatModelProvider.class)
        );

        assertEquals("huggingface", registry.defaultProvider());
        assertEquals(java.util.List.of("huggingface", "ollama"), registry.supportedProviders());
        assertInstanceOf(HuggingFaceChatModelProvider.class, registry.get("huggingface"));
    }

    @Test
    void registryRejectsUnsupportedDefaultProviderNames() {
        assertThrows(IllegalStateException.class, () -> modelProviderConfig.chatModelProviderRegistry(
                new AppModelProperties("unsupported"),
                modelProviderConfig.ollamaChatModelProvider(new OllamaClient(new ObjectMapper(), new OllamaProperties("http://localhost:11434", "llama3:8b", 10, 10))),
                new org.springframework.beans.factory.support.StaticListableBeanFactory().getBeanProvider(ChatModelProvider.class),
                new org.springframework.beans.factory.support.StaticListableBeanFactory().getBeanProvider(ChatModelProvider.class)
        ));
    }

    private static final class FakeBedrockRuntimeGateway implements BedrockRuntimeGateway {

        @Override
        public ModelProviderReply converse(String prompt, String modelId) {
            return new ModelProviderReply("ok", new ModelProviderMetadata("bedrock", modelId, null, null, null, null, null, null, null, null));
        }

        @Override
        public ModelProviderReply converse(java.util.List<ProviderPromptMessage> messages, String modelId) {
            return new ModelProviderReply("ok", new ModelProviderMetadata("bedrock", modelId, null, null, null, null, null, null, null, null));
        }

        @Override
        public CompletableFuture<ModelProviderMetadata> converseStream(String prompt, String modelId, java.util.function.Consumer<String> chunkConsumer) {
            return CompletableFuture.completedFuture(
                    new ModelProviderMetadata("bedrock", modelId, null, null, null, null, null, null, null, null)
            );
        }

        @Override
        public CompletableFuture<ModelProviderMetadata> converseStream(
                java.util.List<ProviderPromptMessage> messages,
                String modelId,
                java.util.function.Consumer<String> chunkConsumer
        ) {
            return CompletableFuture.completedFuture(
                    new ModelProviderMetadata("bedrock", modelId, null, null, null, null, null, null, null, null)
            );
        }
    }
}
