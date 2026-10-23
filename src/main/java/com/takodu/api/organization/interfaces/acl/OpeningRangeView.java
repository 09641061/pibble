package com.takodu.api.organization.interfaces.acl;

import java.time.LocalTime;

/**
 * One opening range in the establishment's local time; it never crosses midnight (O8).
 *
 * @param isoWeekday 1 is Monday, 7 is Sunday
 */
public record OpeningRangeView(int isoWeekday, LocalTime opensAt, LocalTime closesAt) { }
