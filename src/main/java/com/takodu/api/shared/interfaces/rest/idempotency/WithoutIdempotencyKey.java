package com.takodu.api.shared.interfaces.rest.idempotency;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a POST endpoint that takes no {@code Idempotency-Key} header because a repeat is already harmless or
 * deduplicated another way, such as the Meta webhook (by {@code wamid}) or requesting a sign-in code.
 *
 * <p>{@code ArchitectureTest} requires every other POST to take {@code @HeaderParam("Idempotency-Key") UUID}.</p>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface WithoutIdempotencyKey {

    /**
     * @return why a repeated request does no harm
     */
    String value();
}
