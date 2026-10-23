package com.takodu.api.organization.interfaces.acl;

import java.util.List;
import java.util.UUID;

/**
 * A service as other contexts see it (O14, O15).
 *
 * @param priceCents price in the smallest unit of {@code currency}
 * @param performerIds active, bookable members working at the establishment, in the team's order: the order they
 *        joined
 * @param instructions what the customer should know before coming, such as {@code Venir sin maquillaje}; null when
 *        none
 */
public record ServiceView(UUID id, UUID establishmentId, String name, int durationMinutes, long priceCents,
                          String currency, boolean active, List<UUID> performerIds, String instructions) { }
