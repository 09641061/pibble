package com.takodu.api.customers.interfaces.rest;

import com.takodu.api.customers.application.commandservices.CustomerCommandService;
import com.takodu.api.customers.application.queryservices.CustomerQueryService;
import com.takodu.api.customers.domain.exceptions.CustomerNotFoundException;
import com.takodu.api.customers.domain.model.commands.DeleteCustomerCommand;
import com.takodu.api.customers.domain.model.queries.GetCustomerByIdQuery;
import com.takodu.api.customers.interfaces.rest.resources.CustomerDetailsResource;
import com.takodu.api.customers.interfaces.rest.resources.CustomerResource;
import com.takodu.api.customers.interfaces.rest.resources.RegisterCustomerResource;
import com.takodu.api.customers.interfaces.rest.transform.CustomerResourceFromEntityAssembler;
import com.takodu.api.customers.interfaces.rest.transform.RegisterCustomerCommandFromResourceAssembler;
import com.takodu.api.customers.interfaces.rest.transform.UpdateCustomerCommandFromResourceAssembler;
import com.takodu.api.shared.application.tenancy.TenantContext;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.UUID;

@Path("/api/v1/customers")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Customers", description = "The people the organization serves, identified by phone")
public class CustomersController {
    private final CustomerCommandService customerCommandService;
    private final CustomerQueryService customerQueryService;
    private final TenantContext tenantContext;

    public CustomersController(CustomerCommandService customerCommandService, CustomerQueryService customerQueryService,
                               TenantContext tenantContext) {
        this.customerCommandService = customerCommandService;
        this.customerQueryService = customerQueryService;
        this.tenantContext = tenantContext;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @Operation(summary = "Register customer", description = "Any member (C3). Works while the plan is inactive.")
    @APIResponse(responseCode = "201", description = "Customer registered, or the one an earlier request with the same key registered",
            content = @Content(schema = @Schema(implementation = CustomerResource.class)))
    @APIResponse(responseCode = "400", description = "Phone without country code, invalid document, or missing Idempotency-Key")
    @APIResponse(responseCode = "409", description = "CUSTOMER_CONFLICT: an active customer has the phone (C1); or the same request is still running")
    public CustomerResource register(@HeaderParam("Idempotency-Key") UUID idempotencyKey,
                                     @Valid RegisterCustomerResource resource) {
        var command = RegisterCustomerCommandFromResourceAssembler.toCommandFromResource(tenantContext.organizationId(),
                idempotencyKey, resource);
        return CustomerResourceFromEntityAssembler.toResourceFromEntity(customerCommandService.handle(command));
    }

    @GET
    @Path("/{customerId}")
    @Operation(summary = "Get customer by id")
    @APIResponse(responseCode = "200", description = "Customer found",
            content = @Content(schema = @Schema(implementation = CustomerResource.class)))
    @APIResponse(responseCode = "404", description = "Customer deleted or not in the organization")
    public CustomerResource getCustomerById(@PathParam("customerId") UUID customerId) {
        var id = new CustomerId(customerId);
        var customer = customerQueryService.handle(new GetCustomerByIdQuery(tenantContext.organizationId(), id))
                .orElseThrow(() -> new CustomerNotFoundException(id));
        return CustomerResourceFromEntityAssembler.toResourceFromEntity(customer);
    }

    @PUT
    @Path("/{customerId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update customer", description = "Any member (C4). Fields left out are cleared.")
    @APIResponse(responseCode = "200", description = "Customer updated",
            content = @Content(schema = @Schema(implementation = CustomerResource.class)))
    @APIResponse(responseCode = "400", description = "Invalid document, or name or notes too long")
    @APIResponse(responseCode = "404", description = "Customer deleted or not in the organization")
    public CustomerResource update(@PathParam("customerId") UUID customerId, @Valid CustomerDetailsResource resource) {
        var command = UpdateCustomerCommandFromResourceAssembler.toCommandFromResource(tenantContext.organizationId(),
                customerId, resource);
        return CustomerResourceFromEntityAssembler.toResourceFromEntity(customerCommandService.handle(command));
    }

    @DELETE
    @Path("/{customerId}")
    @ResponseStatus(204)
    @Operation(summary = "Delete customer", description = "Owner or manager. Soft delete: history is kept and the phone can be registered again (C5).")
    @APIResponse(responseCode = "204", description = "Customer deleted")
    @APIResponse(responseCode = "403", description = "The caller is staff")
    @APIResponse(responseCode = "404", description = "Customer already deleted or not in the organization")
    public void delete(@PathParam("customerId") UUID customerId) {
        customerCommandService.handle(new DeleteCustomerCommand(tenantContext.organizationId(), new CustomerId(customerId)));
    }
}
