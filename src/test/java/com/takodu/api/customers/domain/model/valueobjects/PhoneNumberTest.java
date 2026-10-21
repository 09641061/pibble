package com.takodu.api.customers.domain.model.valueobjects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhoneNumberTest {

    @ParameterizedTest
    @ValueSource(strings = {"987654321", "987 654 321", "987-654-321", "+51 987 654 321", "0051987654321", "+51 (987) 654.321"})
    void peruvianMobileIsReadAsStaffTypeIt(String typed) {
        assertEquals("+51987654321", PhoneNumber.typed(typed).e164());
    }

    @Test
    void foreignNumberKeepsItsCountryCode() {
        assertEquals("+573001234567", PhoneNumber.typed("+57 300 123 4567").e164());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "12345678", "887654321", "51987654321", "+51 98a 654 321", "+0987654321", "+51"})
    void numberWithoutCountryCodeOrWithLettersIsRefused(String typed) {
        assertThrows(IllegalArgumentException.class, () -> PhoneNumber.typed(typed));
    }

    @Test
    void whatsappIdGetsThePlus() {
        assertEquals("+51987654321", PhoneNumber.fromWhatsappId("51987654321").e164());
    }
}
