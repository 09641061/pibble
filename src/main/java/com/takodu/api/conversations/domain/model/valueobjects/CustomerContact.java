package com.takodu.api.conversations.domain.model.valueobjects;

import com.takodu.api.shared.domain.model.valueobjects.CustomerId;

/**
 * The customer of a conversation as the inbox shows them.
 *
 * @param phone E.164, such as {@code +51987654321}
 * @param deleted whether staff deleted the customer; the chat stays (C5)
 */
public record CustomerContact(CustomerId customerId, String phone, String displayName, boolean deleted) {

    /** @return the number as Meta expects it, without {@code +} */
    public String whatsappId() {
        return phone.startsWith("+") ? phone.substring(1) : phone;
    }
}
