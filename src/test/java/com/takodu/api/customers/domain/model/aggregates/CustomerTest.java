package com.takodu.api.customers.domain.model.aggregates;

import com.takodu.api.customers.domain.model.valueobjects.PhoneNumber;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerTest {
    private final OrganizationId organization = new OrganizationId(UUID.randomUUID());
    private final PhoneNumber phone = new PhoneNumber("+51987654321");

    @Test
    void displayNameFallsBackToWhatsappNameThenPhone() {
        assertEquals("+51987654321", customer(null, null).displayName());
        assertEquals("Rosa 🌸", customer(null, "Rosa 🌸").displayName());
        assertEquals("Rosa Quispe", customer("Rosa Quispe", "Rosa 🌸").displayName());
    }

    @Test
    void blankDetailsAreStoredAsMissing() {
        var customer = Customer.registerByStaff(organization, phone, "  ", null, " ", new IdempotencyKey(UUID.randomUUID()));

        assertNull(customer.getName());
        assertNull(customer.getNotes());
    }

    @Test
    void nameHasAtMost120Characters() {
        assertThrows(IllegalArgumentException.class, () -> customer("x".repeat(121), null));
    }

    private Customer customer(String name, String whatsappProfileName) {
        return new Customer(new CustomerId(UUID.randomUUID()), organization, phone, name, whatsappProfileName, null,
                null, null, null);
    }
}
