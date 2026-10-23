package com.takodu.api.conversations.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static com.takodu.api.conversations.domain.model.valueobjects.DeliveryStatus.DELIVERED;
import static com.takodu.api.conversations.domain.model.valueobjects.DeliveryStatus.FAILED;
import static com.takodu.api.conversations.domain.model.valueobjects.DeliveryStatus.PENDING;
import static com.takodu.api.conversations.domain.model.valueobjects.DeliveryStatus.READ;
import static com.takodu.api.conversations.domain.model.valueobjects.DeliveryStatus.RECEIVED;
import static com.takodu.api.conversations.domain.model.valueobjects.DeliveryStatus.SENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DeliveryStatusTest {

    @Test
    void movesOnlyForward() {
        assertEquals(DELIVERED, SENT.advanceTo(DELIVERED));
        assertEquals(READ, SENT.advanceTo(READ));
        assertEquals(DELIVERED, DELIVERED.advanceTo(SENT));
        assertEquals(READ, READ.advanceTo(DELIVERED));
    }

    @Test
    void failureReplacesAnyStatusThatIsNotFinal() {
        assertEquals(FAILED, PENDING.advanceTo(FAILED));
        assertEquals(FAILED, DELIVERED.advanceTo(FAILED));
        assertEquals(READ, READ.advanceTo(FAILED));
        assertEquals(FAILED, FAILED.advanceTo(READ));
    }

    @Test
    void receivedMessagesHaveNoDeliveryStatus() {
        assertEquals(RECEIVED, RECEIVED.advanceTo(READ));
    }
}
