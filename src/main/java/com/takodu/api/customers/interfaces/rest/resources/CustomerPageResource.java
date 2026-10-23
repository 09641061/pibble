package com.takodu.api.customers.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(name = "CustomerPage")
public record CustomerPageResource(List<CustomerResource> items, int page, int size,
                                   @Schema(description = "Whether the next page has customers") boolean hasMore) { }
