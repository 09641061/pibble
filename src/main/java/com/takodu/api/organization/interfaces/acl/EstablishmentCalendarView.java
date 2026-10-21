package com.takodu.api.organization.interfaces.acl;

import java.util.List;
import java.util.UUID;

/**
 * Where and when an establishment accepts appointments.
 *
 * @param timeZone IANA name, such as {@code America/Lima} (O7)
 * @param openingHours every range of the week; a weekday without ranges is closed (O8)
 * @param archived whether the establishment is archived and takes no new appointments (O4)
 * @param planActive whether the organization's plan is active now (O5, O6)
 */
public record EstablishmentCalendarView(UUID establishmentId, String timeZone, List<OpeningRangeView> openingHours,
                                        boolean archived, boolean planActive) { }
