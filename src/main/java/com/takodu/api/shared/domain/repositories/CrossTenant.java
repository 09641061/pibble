package com.takodu.api.shared.domain.repositories;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a repository port method that is not scoped by organization because its caller has no tenant yet.
 *
 * <p>Jobs and webhooks use such methods to claim work across organizations or to find the row that tells them the
 * organization (the WhatsApp line by {@code phone_number_id}). A signed-in user who has not chosen an organization
 * yet uses them too: listing their memberships, accepting an invitation by its token. {@code ArchitectureTest}
 * requires every other repository method to take an {@code OrganizationId}, an {@code EstablishmentId} or an
 * aggregate.</p>
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
