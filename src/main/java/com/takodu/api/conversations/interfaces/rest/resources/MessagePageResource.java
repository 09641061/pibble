package com.takodu.api.conversations.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

/**
 * @param items newest first
 * @param nextCursor pass it as {@code before} for older messages; null when there are none
 */
@Schema(name = "MessagePage")
public record MessagePageResource(List<MessageResource> items, String nextCursor) { }
