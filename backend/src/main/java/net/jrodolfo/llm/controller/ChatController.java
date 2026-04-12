package net.jrodolfo.llm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.jrodolfo.llm.client.ModelProviderException;
import net.jrodolfo.llm.client.OllamaClientException;
import net.jrodolfo.llm.dto.ChatRequest;
import net.jrodolfo.llm.dto.ChatResponse;
import net.jrodolfo.llm.dto.ChatStreamEvent;
import net.jrodolfo.llm.provider.ChatModelProvider;
import net.jrodolfo.llm.service.ChatOrchestratorService;
import net.jrodolfo.llm.service.InvalidSessionIdException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "chat", description = "Normal and streaming chat endpoints.")
public class ChatController {

    private final ChatOrchestratorService chatOrchestratorService;
    private final ChatModelProvider chatModelProvider;
    private final ObjectMapper objectMapper;

    public ChatController(
            ChatOrchestratorService chatOrchestratorService,
            ChatModelProvider chatModelProvider,
            ObjectMapper objectMapper
    ) {
        this.chatOrchestratorService = chatOrchestratorService;
        this.chatModelProvider = chatModelProvider;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @Operation(summary = "Run a non-streaming chat request", description = "Routes the request through the active provider, optionally using local MCP-backed tools before the model call.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chat response returned successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid request body."),
            @ApiResponse(responseCode = "502", description = "Provider or MCP integration failed.")
    })
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return chatOrchestratorService.chat(request.message(), request.model(), request.sessionId());
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Run a streaming chat request",
            description = "Streams Server-Sent Events using a typed JSON envelope. The stream emits `chat` events with `type=start`, zero or more `type=delta` chunks, and a final `type=complete` event."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "SSE stream started successfully.",
                    content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                            examples = @ExampleObject(value = "event: chat\\ndata: {\"type\":\"start\",\"sessionId\":\"session-123\"}\\n\\nevent: chat\\ndata: {\"type\":\"delta\",\"text\":\"Hello\"}\\n\\nevent: chat\\ndata: {\"type\":\"complete\",\"sessionId\":\"session-123\"}\\n\\n"))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request body."),
            @ApiResponse(responseCode = "502", description = "Provider or MCP integration failed.")
    })
    public SseEmitter stream(@Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L);

        CompletableFuture.runAsync(() -> {
            try {
                ChatOrchestratorService.PreparedChat preparedChat = chatOrchestratorService.prepareChat(
                        request.message(),
                        request.model(),
                        request.sessionId()
                );

                sendEvent(emitter, ChatStreamEvent.start(
                        preparedChat.immediateResponse() != null ? preparedChat.immediateResponse().sessionId() : preparedChat.session().sessionId(),
                        preparedChat.toolMetadata(),
                        preparedChat.toolResult(),
                        preparedChat.pendingTool(),
                        preparedChat.immediateResponse() != null ? preparedChat.immediateResponse().metadata() : null
                ));

                if (preparedChat.immediateResponse() != null) {
                    sendEvent(emitter, ChatStreamEvent.delta(preparedChat.immediateResponse().response()));
                    sendEvent(emitter, ChatStreamEvent.complete(
                            preparedChat.immediateResponse().sessionId(),
                            preparedChat.immediateResponse().tool(),
                            preparedChat.immediateResponse().toolResult(),
                            preparedChat.immediateResponse().pendingTool(),
                            preparedChat.immediateResponse().metadata()
                    ));
                    emitter.complete();
                    return;
                }

                StringBuilder responseBuffer = new StringBuilder();
                var streamingResult = chatModelProvider.streamChat(preparedChat.prompt(), preparedChat.model(), token -> {
                    responseBuffer.append(token);
                    sendEvent(emitter, ChatStreamEvent.delta(token));
                });
                var providerMetadata = streamingResult.completion().join();
                chatOrchestratorService.completePreparedChat(preparedChat, responseBuffer.toString(), providerMetadata);
                sendEvent(emitter, ChatStreamEvent.complete(
                        preparedChat.session().sessionId(),
                        preparedChat.toolMetadata(),
                        preparedChat.toolResult(),
                        preparedChat.pendingTool(),
                        providerMetadata
                ));
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        });

        return emitter;
    }

    @ExceptionHandler(OllamaClientException.class)
    @Operation(hidden = true)
    public ResponseEntity<Map<String, String>> handleOllamaError(OllamaClientException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ModelProviderException.class)
    @Operation(hidden = true)
    public ResponseEntity<Map<String, String>> handleModelProviderError(ModelProviderException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidSessionIdException.class)
    @Operation(hidden = true)
    public ResponseEntity<Map<String, String>> handleInvalidSessionId(InvalidSessionIdException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    private void sendEvent(SseEmitter emitter, ChatStreamEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .name("chat")
                    .data(objectMapper.writeValueAsString(event)));
        } catch (IOException ex) {
            throw new OllamaClientException("Failed to stream chat event to client.", ex);
        }
    }
}
