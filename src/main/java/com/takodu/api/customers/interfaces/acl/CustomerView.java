package com.takodu.api.customers.interfaces.acl;

import java.util.UUID;

/**
 * A customer as other contexts see it.
 *
 * @param phone E.164, such as {@code +51987654321}
 * @param displayName the typed name, else the WhatsApp profile name, else the phone
 * @param deleted whether staff deleted the customer; history still shows them (C5)
 */
public record CustomerView(UUID id, String phone, String displayName, boolean deleted) { }
