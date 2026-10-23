-- C5 and 07 §4: media of a deleted customer, and media older than takodu.media.retention-days, is deleted from disk
-- by the retention job. 'removing' keeps the path until the file is gone; 'removed' keeps the message without it.
ALTER TABLE messages
    DROP CONSTRAINT messages_media_status_ck,
    DROP CONSTRAINT messages_media_download_ck;

ALTER TABLE messages
    ADD CONSTRAINT messages_media_status_ck
        CHECK (media_status IN ('pending', 'ready', 'failed', 'removing', 'removed')),
    ADD CONSTRAINT messages_media_download_ck
        CHECK ((media_id IS NULL) = (media_status IS NULL)
            AND (media_path IS NULL OR media_status IN ('ready', 'removing'))
            AND (media_status <> 'ready' OR media_path IS NOT NULL)
            AND (media_status <> 'pending' OR media_next_attempt_at IS NOT NULL));

CREATE INDEX messages_media_retention_idx ON messages (occurred_at)
    WHERE media_status IN ('pending', 'ready', 'failed', 'removing');
