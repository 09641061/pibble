package com.takodu.api.assistant.infrastructure.llm;

import com.takodu.api.assistant.application.internal.outboundservices.llm.AssistantModel;
import com.takodu.api.assistant.application.internal.outboundservices.llm.AssistantModelException;
import com.takodu.api.assistant.application.internal.outboundservices.llm.ModelAnswer;
import com.takodu.api.assistant.application.internal.outboundservices.llm.TurnPrompt;
import com.takodu.api.assistant.application.internal.tools.BookingTools;
import com.takodu.api.assistant.domain.model.valueobjects.AssistantReply;
import com.takodu.api.assistant.domain.model.valueobjects.ChatLine;
import com.takodu.api.assistant.domain.model.valueobjects.TurnUsage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiTokenUsage;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import io.quarkus.arc.ClientProxy;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * The assistant model with LangChain4j tool calling, in process (08 §4). Each turn builds its own AI service with a
 * fresh memory of the chat, so nothing is kept between turns: the messages table is the only history.
 */
@ApplicationScoped
public class LangChain4jAssistantModel implements AssistantModel {
    /** The chat, plus the tool calls of one turn. */
    private static final int MEMORY_CAPACITY = 60;
    /** A hard stop on model round trips with tools; {@code TurnScope} hands off after 8 tool calls first. */
    private static final int MAX_TOOL_ROUNDS = 10;
    private static final String PROMPT = "prompts/booking-agent.txt";

    private final ChatModel chatModel;
    private final BookingTools bookingTools;
    private final String modelName;
    private final String promptTemplate;

    public LangChain4jAssistantModel(ChatModel chatModel, BookingTools bookingTools,
                                     @ConfigProperty(name = "quarkus.langchain4j.openai.chat-model.model-name")
                                     String modelName) {
        this.chatModel = chatModel;
        this.bookingTools = ClientProxy.unwrap(bookingTools);
        this.modelName = modelName;
        this.promptTemplate = read(PROMPT);
    }

    @Override
    public ModelAnswer answer(TurnPrompt prompt) {
        var memory = MessageWindowChatMemory.builder().maxMessages(MEMORY_CAPACITY).alwaysKeepSystemMessageFirst(true)
                .build();
        prompt.history().forEach(line -> memory.add(message(line)));
        var system = promptTemplate.replace("{establishment}", prompt.establishmentName())
                .replace("{today}", prompt.today())
                .replace("{info}", prompt.businessInfo().isEmpty() ? "(sin información adicional)"
                        : prompt.businessInfo());
        var agent = AiServices.builder(BookingAgent.class)
                .chatModel(chatModel)
                .chatMemory(memory)
                .systemMessage(system)
                .tools(bookingTools)
                .maxSequentialToolsInvocations(MAX_TOOL_ROUNDS)
                .toolExecutionErrorHandler((error, context) -> {
                    throw new AssistantModelException("Tool " + context.toolExecutionRequest().name() + " failed",
                            error);
                })
                .build();
        Result<AgentReply> result;
        try {
            result = agent.reply(prompt.customerMessage().isBlank() ? "(sin texto)" : prompt.customerMessage());
        } catch (AssistantModelException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new AssistantModelException("The language model failed", e);
        }
        var content = result.content();
        if (content == null) throw new AssistantModelException("The language model gave no answer", null);
        try {
            return new ModelAnswer(new AssistantReply(content.text(), content.options()), usage(result),
                    result.toolExecutions().size());
        } catch (IllegalArgumentException e) {
            throw new AssistantModelException("The language model gave an empty answer", e);
        }
    }

    /** Staff's messages are the business speaking, so the model sees them as its own side of the chat, marked. */
    private static ChatMessage message(ChatLine line) {
        return switch (line.role()) {
            case CUSTOMER -> UserMessage.from(line.text());
            case ASSISTANT -> AiMessage.from(line.text());
            case STAFF -> AiMessage.from("[Respondió una persona del equipo] " + line.text());
        };
    }

    private TurnUsage usage(Result<AgentReply> result) {
        var tokens = Objects.requireNonNullElseGet(result.tokenUsage(), TokenUsage::new);
        var cached = tokens instanceof OpenAiTokenUsage openAi && openAi.inputTokensDetails() != null
                ? openAi.inputTokensDetails().cachedTokens() : null;
        var answered = result.finalResponse() == null ? null : result.finalResponse().modelName();
        return new TurnUsage(answered == null ? modelName : answered, orZero(tokens.inputTokenCount()),
                orZero(cached), orZero(tokens.outputTokenCount()));
    }

    private static int orZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static String read(String resource) {
        try (var stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException("Missing " + resource);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
