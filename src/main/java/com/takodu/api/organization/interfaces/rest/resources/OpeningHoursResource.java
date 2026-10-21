package com.takodu.api.organization.interfaces.rest.resources;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(name = "OpeningHours", description = "The whole week; a weekday without ranges is closed (O8)")
public record OpeningHoursResource(@NotNull @Size(max = 70) List<@NotNull @Valid OpeningRangeResource> ranges) { }
