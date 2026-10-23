package com.takodu.api.conversations.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

/** @param nextCursor pass it as {@code cursor} for the next page; null on the last page */
@Schema(name = "ConversationPage")
public record ConversationPageResource(List<ConversationResource> items, String nextCursor) { }
