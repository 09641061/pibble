package com.takodu.api.assistant.infrastructure.llm;

import dev.langchain4j.model.output.structured.Description;

import java.util.List;

/** The model's structured answer, which LangChain4j parses from JSON. */
public record AgentReply(
        @Description("El mensaje para el cliente, breve y en su idioma") String text,
        @Description("Opciones cortas para que el cliente elija, como horarios o servicios; vacío si no hay")
        List<String> options) { }
