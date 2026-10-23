-- A3, 08 §6: a member pauses the assistant until someone resumes it; a retried pause or resume applies once.
ALTER TABLE conversations
    ADD COLUMN assistant_manually_paused_at timestamptz,
    ADD COLUMN assistant_manually_paused_by uuid REFERENCES members (id),
    ADD COLUMN last_idempotency_key         uuid,
    ADD CONSTRAINT conversations_manual_pause_ck
        CHECK ((assistant_manually_paused_at IS NULL) = (assistant_manually_paused_by IS NULL));
