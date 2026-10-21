package com.takodu.api.shared.interfaces.rest.resources;

/**
 * Body of every error response.
 *
 * @param code machine-readable error code; clients branch on this, never on {@code message}
 * @param message human-readable description
 */
public record ErrorResource(String code, String message) {
}
