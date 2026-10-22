package com.takodu.api.assistant.domain.model.valueobjects;

import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.Money;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * An active service the customer can book (A5).
 *
 * @param instructions what the customer should know before coming; null when none
 * @param performers who can perform it, in the team's order
 */
public record OfferedService(ServiceId id, String name, Duration duration, Money price, String instructions,
                             List<MemberId> performers) {
    public OfferedService {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(duration, "duration is required");
        Objects.requireNonNull(price, "price is required");
        performers = List.copyOf(performers);
    }
}
