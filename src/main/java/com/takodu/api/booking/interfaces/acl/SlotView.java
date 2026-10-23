package com.takodu.api.booking.interfaces.acl;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A time the service can be booked at (B16).
 *
 * @param freeMemberIds who can take it, in the order B17 picks them
 */
public record SlotView(Instant start, List<UUID> freeMemberIds) { }
