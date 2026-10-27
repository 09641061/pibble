package com.takodu.api.conversations.interfaces.rest;

import com.takodu.api.conversations.application.queryservices.InboxStreamQueryService;
import com.takodu.api.conversations.domain.model.queries.StreamInboxChangesQuery;
import com.takodu.api.conversations.interfaces.rest.resources.InboxChangeResource;
import com.takodu.api.conversations.interfaces.rest.transform.InboxChangeResourceFromEntityAssembler;
import com.takodu.api.shared.application.tenancy.TenantContext;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestStreamElementType;

import java.time.Duration;

@Path("/api/v1/inbox/stream")
@Tag(name = "Inbox", description = "WhatsApp conversations and messages of each establishment")
public class InboxStreamController {
    private static final Duration PING = Duration.ofSeconds(25);

    private final InboxStreamQueryService inboxStreamQueryService;
    private final TenantContext tenantContext;

    public InboxStreamController(InboxStreamQueryService inboxStreamQueryService, TenantContext tenantContext) {
        this.inboxStreamQueryService = inboxStreamQueryService;
        this.tenantContext = tenantContext;
    }

    /** Blocking because {@code TenantFilter} loads the membership from the database before the stream opens. */
    @GET
    @Blocking
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    @Operation(summary = "Follow the inbox",
            description = "Server-sent events for the establishments the caller sees: each conversation that changed, with its summary for the list, and when the assistant starts (assistant_typing) and stops (assistant_idle) answering one. A ping follows the subscription and repeats every 25 seconds. Reconnect and reload after a drop.")
    @APIResponse(responseCode = "200", description = "An endless event stream")
    public Multi<InboxChangeResource> stream() {
        var query = new StreamInboxChangesQuery(tenantContext.organizationId(), tenantContext.role().seesAllEstablishments(),
                tenantContext.establishmentIds());
        var changes = inboxStreamQueryService.handle(query)
                .map(InboxChangeResourceFromEntityAssembler::toResourceFromEntity);
        var pings = Multi.createFrom().ticks().every(PING).map(tick -> InboxChangeResource.ping());
        // The first ping tells the client the stream is subscribed; changes come first so none is missed before it.
        return Multi.createBy().merging().streams(changes, Multi.createFrom().item(InboxChangeResource.ping()), pings);
    }
}
