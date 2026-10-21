package com.takodu.api.shared.interfaces.rest.errors;

import com.takodu.api.shared.domain.exceptions.AccessDeniedException;
import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;
import com.takodu.api.shared.domain.exceptions.ConflictException;
import com.takodu.api.shared.domain.exceptions.PlanInactiveException;
import com.takodu.api.shared.domain.exceptions.RequestInProgressException;
import com.takodu.api.shared.domain.exceptions.ResourceNotFoundException;
import com.takodu.api.shared.interfaces.rest.resources.ErrorResource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionMapperTest {

    private final GlobalExceptionMapper mapper = new GlobalExceptionMapper();

    @Test
    void notFoundMapsTo404WithResourceCode() {
        var response = mapper.toResponse(new ResourceNotFoundException("Customer", 7));

        assertEquals(404, response.getStatus());
        assertEquals(new ErrorResource("CUSTOMER_NOT_FOUND", "Customer not found: 7"), response.getEntity());
    }

    @Test
    void conflictMapsTo409() {
        assertEquals(409, mapper.toResponse(new ConflictException("Customer", "Phone already registered")).getStatus());
    }

    @Test
    void requestInProgressMapsTo409WithRequestConflictCode() {
        var response = mapper.toResponse(new RequestInProgressException());

        assertEquals(409, response.getStatus());
        assertEquals("REQUEST_CONFLICT", ((ErrorResource) response.getEntity()).code());
    }

    @Test
    void businessRuleViolationMapsTo422() {
        assertEquals(422, mapper.toResponse(new BusinessRuleViolationException("WINDOW_CLOSED", "Closed")).getStatus());
    }

    @Test
    void accessDeniedMapsTo403() {
        var response = mapper.toResponse(new AccessDeniedException("delete customers"));

        assertEquals(403, response.getStatus());
        assertEquals(new ErrorResource("ACCESS_DENIED", "Not allowed to delete customers"), response.getEntity());
    }

    @Test
    void inactivePlanMapsTo402() {
        assertEquals(402, mapper.toResponse(new PlanInactiveException("org-1")).getStatus());
    }

    @Test
    void invalidValueMapsTo400() {
        var response = mapper.toResponse(new IllegalArgumentException("Phone must be in E.164 format"));

        assertEquals(400, response.getStatus());
        assertEquals(new ErrorResource("VALIDATION_ERROR", "Phone must be in E.164 format"), response.getEntity());
    }
}
