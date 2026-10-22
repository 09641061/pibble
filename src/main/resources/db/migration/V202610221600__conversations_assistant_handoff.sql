-- A7: the assistant hands a chat to a person and stays quiet until staff resume it; the inbox shows why.
ALTER TABLE conversations
    ADD COLUMN assistant_handed_off_at timestamptz,
    ADD COLUMN handoff_summary         text,
    ADD CONSTRAINT conversations_handoff_ck CHECK ((assistant_handed_off_at IS NULL) = (handoff_summary IS NULL));
