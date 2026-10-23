package com.takodu.api.organization.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "PlanChange", description = "One entry of an organization's append-only plan history (O5)")
public record PlanChangeResource(UUID id, UUID organizationId,
                                 @Schema(enumeration = {"standard", "max"}) String fromPlan,
                                 @Schema(enumeration = {"standard", "max"}) String toPlan,
                                 Instant fromActiveUntil, Instant toActiveUntil, String note,
                                 @Schema(description = "The platform operator's user id") UUID changedBy,
                                 String changedByEmail, Instant changedAt) { }
