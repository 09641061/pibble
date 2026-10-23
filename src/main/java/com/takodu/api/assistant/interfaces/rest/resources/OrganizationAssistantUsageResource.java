package com.takodu.api.assistant.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(name = "OrganizationAssistantUsage", description = "Each establishment's assistant usage and their sum (A9)")
public record OrganizationAssistantUsageResource(UUID organizationId, LocalDate from, LocalDate to,
                                                 UsageTotalsResource totals,
                                                 @Schema(description = "Establishments with answered turns, by name") List<AssistantUsageResource> establishments) { }
