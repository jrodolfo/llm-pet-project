package net.jrodolfo.llm.client;

import org.junit.jupiter.api.Test;
import net.jrodolfo.llm.dto.ModelProviderMetadata;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseMetrics;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseOutput;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlockDelta;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlockDeltaEvent;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamResponseHandler;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.StopReason;
import software.amazon.awssdk.services.bedrockruntime.model.TokenUsage;

import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AwsSdkBedrockRuntimeGatewayTest {

    @Test
    void converseMapsBedrockMetadata() {
        BedrockRuntimeClient syncClient = syncClient(ConverseResponse.builder()
                .stopReason(StopReason.END_TURN)
                .usage(TokenUsage.builder().inputTokens(12).outputTokens(34).totalTokens(46).build())
                .metrics(ConverseMetrics.builder().latencyMs(321L).build())
                .output(ConverseOutput.builder()
                        .message(Message.builder()
                                .content(software.amazon.awssdk.services.bedrockruntime.model.ContentBlock.fromText("hello from bedrock"))
                                .build())
                        .build())
                .build());
        AwsSdkBedrockRuntimeGateway gateway = new AwsSdkBedrockRuntimeGateway(syncClient, asyncClient((request, handler) -> CompletableFuture.completedFuture(null)));

        ModelProviderReply reply = gateway.converse("prompt", "amazon.nova-lite-v1:0");

        assertEquals("hello from bedrock", reply.text());
        ModelProviderMetadata metadata = reply.metadata();
        assertEquals("bedrock", metadata.provider());
        assertEquals("amazon.nova-lite-v1:0", metadata.modelId());
        assertEquals("end_turn", metadata.stopReason());
        assertEquals(12, metadata.inputTokens());
        assertEquals(34, metadata.outputTokens());
        assertEquals(46, metadata.totalTokens());
        assertEquals(321L, metadata.providerLatencyMs());
    }

    @Test
    void forwardChunkAddsNonBlankText() throws Exception {
        AwsSdkBedrockRuntimeGateway gateway = new AwsSdkBedrockRuntimeGateway(syncClient(), asyncClient((request, handler) -> CompletableFuture.completedFuture(null)));
        List<String> chunks = new ArrayList<>();
        invokeForwardChunk(
                gateway,
                ContentBlockDeltaEvent.builder()
                        .delta(ContentBlockDelta.builder().text("hello").build())
                        .build(),
                chunks::add
        );

        assertEquals(List.of("hello"), chunks);
    }

    @Test
    void forwardChunkIgnoresBlankText() throws Exception {
        AwsSdkBedrockRuntimeGateway gateway = new AwsSdkBedrockRuntimeGateway(syncClient(), asyncClient((request, handler) -> CompletableFuture.completedFuture(null)));
        List<String> chunks = new ArrayList<>();
        invokeForwardChunk(
                gateway,
                ContentBlockDeltaEvent.builder()
                        .delta(ContentBlockDelta.builder().text(" ").build())
                        .build(),
                chunks::add
        );

        assertEquals(List.of(), chunks);
    }

    @Test
    void converseStreamWrapsAsyncClientFailures() {
        BedrockRuntimeAsyncClient asyncClient = asyncClient((request, handler) -> {
            RuntimeException error = new RuntimeException("boom");
            handler.exceptionOccurred(error);
            return CompletableFuture.failedFuture(error);
        });

        AwsSdkBedrockRuntimeGateway gateway = new AwsSdkBedrockRuntimeGateway(syncClient(), asyncClient);

        ModelProviderException exception = assertThrows(
                ModelProviderException.class,
                () -> gateway.converseStream("prompt", "amazon.nova-lite-v1:0", chunk -> {
                })
        );

        assertEquals("Failed to stream from Bedrock.", exception.getMessage());
    }

    private BedrockRuntimeClient syncClient() {
        return syncClient(null);
    }

    private BedrockRuntimeClient syncClient(ConverseResponse converseResponse) {
        return (BedrockRuntimeClient) Proxy.newProxyInstance(
                BedrockRuntimeClient.class.getClassLoader(),
                new Class<?>[]{BedrockRuntimeClient.class},
                (proxy, method, args) -> {
                    if ("converse".equals(method.getName())) {
                        return converseResponse;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private BedrockRuntimeAsyncClient asyncClient(ConverseStreamInvocation invocation) {
        return (BedrockRuntimeAsyncClient) Proxy.newProxyInstance(
                BedrockRuntimeAsyncClient.class.getClassLoader(),
                new Class<?>[]{BedrockRuntimeAsyncClient.class},
                (proxy, method, args) -> {
                    if ("converseStream".equals(method.getName())) {
                        return invocation.invoke(
                                (ConverseStreamRequest) args[0],
                                (ConverseStreamResponseHandler) args[1]
                        );
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType.equals(Boolean.TYPE)) {
            return false;
        }
        if (returnType.equals(Integer.TYPE)) {
            return 0;
        }
        if (returnType.equals(Long.TYPE)) {
            return 0L;
        }
        if (returnType.equals(Float.TYPE)) {
            return 0f;
        }
        if (returnType.equals(Double.TYPE)) {
            return 0d;
        }
        return null;
    }

    private void invokeForwardChunk(
            AwsSdkBedrockRuntimeGateway gateway,
            ContentBlockDeltaEvent event,
            java.util.function.Consumer<String> consumer
    ) throws Exception {
        Method method = AwsSdkBedrockRuntimeGateway.class.getDeclaredMethod(
                "forwardChunk",
                ContentBlockDeltaEvent.class,
                java.util.function.Consumer.class
        );
        method.setAccessible(true);
        method.invoke(gateway, event, consumer);
    }

    @FunctionalInterface
    private interface ConverseStreamInvocation {
        CompletableFuture<Void> invoke(ConverseStreamRequest request, ConverseStreamResponseHandler handler);
    }
}
