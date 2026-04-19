package net.jrodolfo.llm.provider;

import net.jrodolfo.llm.client.HuggingFaceClient;
import net.jrodolfo.llm.client.ModelProviderException;
import net.jrodolfo.llm.client.ModelProviderReply;
import net.jrodolfo.llm.config.HuggingFaceProperties;
import net.jrodolfo.llm.dto.ChatResponse;
import net.jrodolfo.llm.dto.ModelProviderMetadata;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HuggingFaceChatModelProviderTest {

    @Test
    void chatUsesConfiguredDefaultModelWhenRequestModelIsBlank() {
        FakeHuggingFaceClient client = new FakeHuggingFaceClient();
        HuggingFaceChatModelProvider provider = new HuggingFaceChatModelProvider(
                client,
                new HuggingFaceProperties(
                        "https://router.huggingface.co/v1/chat/completions",
                        "token",
                        "meta-llama/Llama-3.1-8B-Instruct",
                        List.of("meta-llama/Llama-3.1-8B-Instruct", "Qwen/Qwen2.5-72B-Instruct"),
                        10,
                        60
                )
        );

        ChatResponse response = provider.chat(ProviderPrompt.forPrompt("Explain recursion."), " ", null, null, "session-1", null);

        assertEquals("hf response", response.response());
        assertEquals("huggingface", response.metadata().provider());
        assertEquals("meta-llama/Llama-3.1-8B-Instruct", response.metadata().modelId());
        assertEquals(List.of(new ProviderPromptMessage("user", "Explain recursion.")), client.lastMessages);
    }

    @Test
    void chatPreservesStructuredMessagesWhenPromptContainsHistory() {
        FakeHuggingFaceClient client = new FakeHuggingFaceClient();
        HuggingFaceChatModelProvider provider = new HuggingFaceChatModelProvider(
                client,
                new HuggingFaceProperties(
                        "https://router.huggingface.co/v1/chat/completions",
                        "token",
                        "meta-llama/Llama-3.1-8B-Instruct",
                        List.of("meta-llama/Llama-3.1-8B-Instruct"),
                        10,
                        60
                )
        );

        provider.chat(
                ProviderPrompt.forMessages(
                        List.of(
                                new ProviderPromptMessage("system", "You are helpful."),
                                new ProviderPromptMessage("user", "Explain recursion."),
                                new ProviderPromptMessage("assistant", "Recursion calls itself."),
                                new ProviderPromptMessage("user", "Now use Fibonacci.")
                        ),
                        "fallback"
                ),
                "meta-llama/Llama-3.1-8B-Instruct",
                null,
                null,
                "session-1",
                null
        );

        assertEquals(List.of(
                new ProviderPromptMessage("system", "You are helpful."),
                new ProviderPromptMessage("user", "Explain recursion."),
                new ProviderPromptMessage("assistant", "Recursion calls itself."),
                new ProviderPromptMessage("user", "Now use Fibonacci.")
        ), client.lastMessages);
    }

    @Test
    void streamChatEmitsFullResponseAsOneChunk() {
        FakeHuggingFaceClient client = new FakeHuggingFaceClient();
        HuggingFaceChatModelProvider provider = new HuggingFaceChatModelProvider(
                client,
                new HuggingFaceProperties(
                        "https://router.huggingface.co/v1/chat/completions",
                        "token",
                        "meta-llama/Llama-3.1-8B-Instruct",
                        List.of("meta-llama/Llama-3.1-8B-Instruct"),
                        10,
                        60
                )
        );
        List<String> chunks = new ArrayList<>();

        StreamingChatResult result = provider.streamChat(ProviderPrompt.forPrompt("Explain recursion."), "meta-llama/Llama-3.1-8B-Instruct", chunks::add);

        assertEquals(List.of("hf response"), chunks);
        assertEquals("huggingface", result.completion().join().provider());
    }

    @Test
    void resolveModelRejectsModelsOutsideCuratedList() {
        HuggingFaceChatModelProvider provider = new HuggingFaceChatModelProvider(
                new FakeHuggingFaceClient(),
                new HuggingFaceProperties(
                        "https://router.huggingface.co/v1/chat/completions",
                        "token",
                        "meta-llama/Llama-3.1-8B-Instruct",
                        List.of("meta-llama/Llama-3.1-8B-Instruct"),
                        10,
                        60
                )
        );

        assertThrows(ModelProviderException.class, () -> provider.resolveModel("Qwen/Qwen2.5-72B-Instruct"));
    }

    private static final class FakeHuggingFaceClient extends HuggingFaceClient {
        private List<ProviderPromptMessage> lastMessages = List.of();

        private FakeHuggingFaceClient() {
            super(
                    new com.fasterxml.jackson.databind.ObjectMapper(),
                    new HuggingFaceProperties(
                            "https://router.huggingface.co/v1/chat/completions",
                            "token",
                            "meta-llama/Llama-3.1-8B-Instruct",
                            List.of("meta-llama/Llama-3.1-8B-Instruct"),
                            10,
                            60
                    )
            );
        }

        @Override
        public ModelProviderReply chat(List<ProviderPromptMessage> messages, String model) {
            lastMessages = List.copyOf(messages);
            return new ModelProviderReply(
                    "hf response",
                    new ModelProviderMetadata("huggingface", model, "stop", 10, 20, 30, 400L, null, null, null)
            );
        }
    }
}
