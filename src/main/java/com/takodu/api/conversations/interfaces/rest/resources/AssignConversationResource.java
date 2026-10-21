package com.takodu.api.conversations.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "AssignConversationRequest", description = "W9: informative only; it locks nothing")
public record AssignConversationResource(@Schema(description = "Leave out to unassign") UUID memberId) { }
