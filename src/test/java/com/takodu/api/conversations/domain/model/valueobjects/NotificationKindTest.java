package com.takodu.api.conversations.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationKindTest {

    @Test
    void reminderAlwaysGoesAsTheTemplateAndTheOthersOnlyOutsideTheWindow() {
        assertTrue(NotificationKind.REMINDER.goesAsTemplate(true));
        assertTrue(NotificationKind.REMINDER.goesAsTemplate(false));
        assertFalse(NotificationKind.CONFIRMATION.goesAsTemplate(true));
        assertTrue(NotificationKind.CONFIRMATION.goesAsTemplate(false));
        assertTrue(NotificationKind.CANCELLATION.goesAsTemplate(false));
    }
}
