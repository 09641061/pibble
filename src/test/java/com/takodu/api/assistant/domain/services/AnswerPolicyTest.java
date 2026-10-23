package com.takodu.api.assistant.domain.services;

import com.takodu.api.assistant.domain.model.aggregates.AssistantSettings;
import com.takodu.api.assistant.domain.model.valueobjects.ConversationId;
import com.takodu.api.assistant.domain.model.valueobjects.ConversationState;
import com.takodu.api.assistant.domain.model.valueobjects.EstablishmentProfile;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnswerPolicyTest {
    private final EstablishmentId establishmentId = new EstablishmentId(UUID.randomUUID());

    @Test
    void answersWhenEverythingHolds() {
        assertEquals(Optional.empty(), AnswerPolicy.whyQuiet(enabled(), establishment(false, true),
                conversation(true, true, false)));
    }

    @Test
    void staysQuietForEachMissingCondition() {
        assertEquals(Optional.of("The assistant is off"), AnswerPolicy.whyQuiet(null, establishment(false, true),
                conversation(true, true, false)));
        assertEquals(Optional.of("The plan is inactive"), AnswerPolicy.whyQuiet(enabled(), establishment(false, false),
                conversation(true, true, false)));
        assertEquals(Optional.of("The line is disconnected"), AnswerPolicy.whyQuiet(enabled(),
                establishment(false, true), conversation(false, true, false)));
        assertEquals(Optional.of("The customer service window is closed"), AnswerPolicy.whyQuiet(enabled(),
                establishment(false, true), conversation(true, false, false)));
        assertEquals(Optional.of("A person is handling the chat"), AnswerPolicy.whyQuiet(enabled(),
                establishment(false, true), conversation(true, true, true)));
        assertEquals(Optional.of("The establishment is archived"), AnswerPolicy.whyQuiet(enabled(),
                establishment(true, true), conversation(true, true, false)));
    }

    @Test
    void businessInfoIsLimitedToFourThousandCharacters() {
        var settings = enabled();

        settings.configure(true, "x".repeat(4000));
        assertThrows(IllegalArgumentException.class,
                () -> settings.configure(true, "x".repeat(4001)));
    }

    private AssistantSettings enabled() {
        var settings = AssistantSettings.defaultsFor(new OrganizationId(UUID.randomUUID()), establishmentId);
        settings.configure(true, null);
        return settings;
    }

    private EstablishmentProfile establishment(boolean archived, boolean planActive) {
        return new EstablishmentProfile(establishmentId, "Sede Miraflores", null, ZoneId.of("America/Lima"), List.of(),
                archived, planActive);
    }

    private ConversationState conversation(boolean lineConnected, boolean windowOpen, boolean paused) {
        return new ConversationState(new ConversationId(UUID.randomUUID()), establishmentId,
                new CustomerId(UUID.randomUUID()), lineConnected, windowOpen, paused);
    }
}
