package com.takodu.api.organization.interfaces.acl;

import java.util.UUID;

/**
 * An active member as other contexts see it.
 *
 * @param worksThere whether the member works at the establishment that was asked about (O13)
 */
public record MemberView(UUID memberId, String displayName, boolean worksThere) { }
