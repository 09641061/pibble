package com.takodu.api.conversations.infrastructure.meta;

import com.takodu.api.conversations.domain.model.valueobjects.TemplateStatus;

import java.util.Optional;

/**
 * Meta's template statuses and status events, checked against Meta's docs on 2026-10-22. {@code FLAGGED} (quality
 * dropped, still sendable) and {@code UNARCHIVED} change nothing Takodu tracks.
 */
final class MetaTemplateStatuses {
    private MetaTemplateStatuses() { }

    static Optional<TemplateStatus> of(String status) {
        return Optional.ofNullable(switch (status == null ? "" : status) {
            case "APPROVED", "REINSTATED" -> TemplateStatus.APPROVED;
            case "PENDING", "IN_APPEAL" -> TemplateStatus.PENDING;
            case "REJECTED" -> TemplateStatus.REJECTED;
            case "PAUSED", "LOCKED" -> TemplateStatus.PAUSED;
            case "DISABLED", "DELETED", "ARCHIVED", "PENDING_DELETION", "LIMIT_EXCEEDED" -> TemplateStatus.DISABLED;
            default -> null;
        });
    }
}
