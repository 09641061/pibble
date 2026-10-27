-- The inbox summary counts only chats with unread messages or a handoff; this keeps it from reading the rest.
CREATE INDEX conversations_attention_idx ON conversations (establishment_id)
    WHERE unread_count > 0 OR assistant_handed_off_at IS NOT NULL;
