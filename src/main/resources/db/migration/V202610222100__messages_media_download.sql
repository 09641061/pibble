-- 07 §4: the media job downloads each message's media; attempts stop at 5 and a failure stays visible.
ALTER TABLE messages
    ADD COLUMN media_status          text,
    ADD COLUMN media_attempts        smallint NOT NULL DEFAULT 0,
    ADD COLUMN media_next_attempt_at timestamptz,
    ADD COLUMN media_error           text;

UPDATE messages SET media_status = 'pending', media_next_attempt_at = occurred_at WHERE media_id IS NOT NULL;

ALTER TABLE messages
    ADD CONSTRAINT messages_media_status_ck CHECK (media_status IN ('pending', 'ready', 'failed')),
    ADD CONSTRAINT messages_media_download_ck
        CHECK ((media_id IS NULL) = (media_status IS NULL)
            AND (media_path IS NULL OR media_status = 'ready')
            AND (media_status <> 'ready' OR media_path IS NOT NULL)
            AND (media_status <> 'pending' OR media_next_attempt_at IS NOT NULL)),
    ADD CONSTRAINT messages_media_attempts_ck CHECK (media_attempts BETWEEN 0 AND 5);

CREATE INDEX messages_media_due_idx ON messages (media_next_attempt_at) WHERE media_status = 'pending';
