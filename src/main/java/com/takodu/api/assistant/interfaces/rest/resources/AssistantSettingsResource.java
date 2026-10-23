package com.takodu.api.assistant.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

/** @param businessInfo what the assistant may tell customers besides services, prices and hours; empty when none */
@Schema(name = "AssistantSettings")
public record AssistantSettingsResource(UUID establishmentId, boolean enabled, String businessInfo) { }
