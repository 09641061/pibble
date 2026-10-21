package com.takodu.api.shared.domain.repositories;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a repository port method that is not scoped by organization because its caller has no tenant yet.
 *
 * <p>Only jobs and webhooks use such methods: they claim work across organizations or find the row that tells
 * them the organization (the WhatsApp line by {@code phone_number_id}). {@code ArchitectureTest} requires every
 * other repository method to take an {@code OrganizationId}, an {@code EstablishmentId} or an aggregate.</p>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface CrossTenant {

    /**
     * @return which job or webhook calls the method, and how it derives the organization from the result
     */
    String value();
}
