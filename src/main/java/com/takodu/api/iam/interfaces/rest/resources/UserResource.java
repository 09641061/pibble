package com.takodu.api.iam.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "User")
public record UserResource(UUID id, String email, String name) { }
