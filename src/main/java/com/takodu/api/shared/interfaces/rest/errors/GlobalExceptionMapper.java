package com.takodu.api.shared.interfaces.rest.errors;

import com.takodu.api.shared.domain.exceptions.AccessDeniedException;
import com.takodu.api.shared.domain.exceptions.AuthenticationFailedException;
import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;
import com.takodu.api.shared.domain.exceptions.ConflictException;
import com.takodu.api.shared.domain.exceptions.DomainException;
import com.takodu.api.shared.domain.exceptions.PlanInactiveException;
import com.takodu.api.shared.domain.exceptions.ResourceNotFoundException;
import com.takodu.api.shared.domain.exceptions.TooManyRequestsException;
import com.takodu.api.shared.interfaces.rest.resources.ErrorResource;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Translates expected failures into HTTP responses with an {@link ErrorResource} body.
 *
 * <p>This is the only place that chooses an HTTP status for a business failure. Unexpected exceptions are
 * not mapped here, so Quarkus logs them and returns 500.</p>
 */
public class GlobalExceptionMapper {

    /**
     * Maps a domain failure to its HTTP status.
     *
     * @param exception the failure thrown by a domain or application class
     * @return 401, 402, 403, 404, 409, 422 or 429 with the exception's code and message
     */
    @ServerExceptionMapper
    public Response toResponse(DomainException exception) {
        int status = switch (exception) {
            case AuthenticationFailedException e -> 401;
            case PlanInactiveException e -> 402;
            case AccessDeniedException e -> 403;
            case ResourceNotFoundException e -> 404;
            case ConflictException e -> 409;
            case BusinessRuleViolationException e -> 422;
            case TooManyRequestsException e -> 429;
            default -> 500;
        };
        return error(status, exception.code(), exception.getMessage());
    }

    /**
     * Maps a rejected value object or command constructor argument.
     *
     * @param exception the validation failure raised in a record's compact constructor
     * @return 400 with code {@code VALIDATION_ERROR}
     */
    @ServerExceptionMapper
    public Response toResponse(IllegalArgumentException exception) {
        return error(400, "VALIDATION_ERROR", exception.getMessage());
    }

    private static Response error(int status, String code, String message) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResource(code, message))
                .build();
    }
}
