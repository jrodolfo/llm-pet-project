package net.jrodolfo.llm.client;

import java.util.function.Consumer;

public interface BedrockRuntimeGateway {

    ModelProviderReply converse(String prompt, String modelId);

    void converseStream(String prompt, String modelId, Consumer<String> chunkConsumer);
}
