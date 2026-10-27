package com.takodu.api.conversations.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * @param awaitingReply chats that wait for a person: handed off by the assistant, or with unread customer messages
 *                      that nobody, not even the assistant, answered yet
 * @param unread chats with at least one unread message
 */
@Schema(name = "InboxSummary")
public record InboxSummaryResource(int awaitingReply, int unread) { }
