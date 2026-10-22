package com.takodu.api.assistant.infrastructure.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiTokenUsage;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Replaces the language model in tests: each call answers with the next scripted step, a tool call or a final
 * reply, so the real tools, booking and outbox run without a provider. Every answer costs 100 input tokens, 40 of
 * them cached, and 20 output tokens.
 */
@Alternative
@Priority(1)
@ApplicationScoped
public class ScriptedChatModel implements ChatModel {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Deque<Function<ChatRequest, AiMessage>> steps = new ConcurrentLinkedDeque<>();
    private final List<ChatRequest> requests = Collections.synchronizedList(new ArrayList<>());

    /** Replaces the script; each step answers one call, in order. */
    @SafeVarargs
    public final void script(Function<ChatRequest, AiMessage>... next) {
        steps.clear();
        requests.clear();
        steps.addAll(List.of(next));
    }

    /** @param arguments the tool's arguments as JSON, such as {@code {"serviceName": "Corte"}} */
    public static Function<ChatRequest, AiMessage> tool(String name, String arguments) {
        return request -> AiMessage.from(ToolExecutionRequest.builder().id(UUID.randomUUID().toString()).name(name)
                .arguments(arguments).build());
    }

    public static Function<ChatRequest, AiMessage> reply(String text, String... options) {
        var json = "{\"text\": \"%s\", \"options\": [%s]}".formatted(text.replace("\"", "\\\""),
                List.of(options).stream().map(option -> "\"" + option + "\"").collect(Collectors.joining(", ")));
        return request -> AiMessage.from(json);
    }

    /** The provider fails, as after a timeout. */
    public static Function<ChatRequest, AiMessage> failure() {
        return request -> {
            throw new IllegalStateException("The provider timed out");
        };
    }

    /** @return the results the tools gave the model in the call, oldest first, as the tools wrote them */
    public static List<String> toolResults(ChatRequest request) {
        return request.messages().stream().filter(ToolExecutionResultMessage.class::isInstance)
                .map(message -> unquote(((ToolExecutionResultMessage) message).text())).toList();
    }

    /** LangChain4j sends a tool's text to the model as a JSON string. */
    private static String unquote(String json) {
        try {
            return JSON.readValue(json, String.class);
        } catch (JsonProcessingException e) {
            return json;
        }
    }

    /** @return every call the model received since the script was set */
    public List<ChatRequest> requests() {
        return List.copyOf(requests);
    }

    /** @return the messages of the last call, in order */
    public List<ChatMessage> lastMessages() {
        return requests.getLast().messages();
    }

    @Override
    public ChatResponse doChat(ChatRequest request) {
        requests.add(request);
        var step = steps.pollFirst();
        if (step == null) throw new IllegalStateException("The script has no answer left");
        return ChatResponse.builder().aiMessage(step.apply(request)).modelName("scripted-model")
                .tokenUsage(OpenAiTokenUsage.builder().inputTokenCount(100).outputTokenCount(20)
                        .inputTokensDetails(OpenAiTokenUsage.InputTokensDetails.builder().cachedTokens(40).build())
                        .build())
                .build();
    }
}
