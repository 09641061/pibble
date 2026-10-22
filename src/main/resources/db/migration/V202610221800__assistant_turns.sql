-- A2, A9: one row per customer message the assistant must answer; the turn worker's queue and the cost record.
-- A turn runs outside any transaction (its tools write through booking in their own), so 'running' with started_at
-- is a lease another worker takes over after 5 minutes. answered_through is the latest customer message a finished
-- turn took into account, so turns for messages it already answered are skipped.
CREATE TABLE assistant_turns (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id    uuid NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    establishment_id   uuid NOT NULL REFERENCES establishments (id) ON DELETE CASCADE,
    conversation_id    uuid NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    customer_id        uuid NOT NULL REFERENCES customers (id),
    trigger_message_id uuid NOT NULL REFERENCES messages (id) ON DELETE CASCADE,
    trigger_kind       text NOT NULL,
    trigger_at         timestamptz NOT NULL,
    status             text NOT NULL DEFAULT 'pending',
    attempts           smallint NOT NULL DEFAULT 0,
    run_after          timestamptz NOT NULL,
    started_at         timestamptz,
    answered_through   timestamptz,
    model              text,
    input_tokens       integer,
    cached_tokens      integer,
    output_tokens      integer,
    tool_calls         smallint NOT NULL DEFAULT 0,
    error              text,
    finished_at        timestamptz,
    created_at         timestamptz NOT NULL DEFAULT now(),
    updated_at         timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT assistant_turns_status_ck CHECK (status IN ('pending', 'running', 'done', 'skipped', 'failed')),
    CONSTRAINT assistant_turns_attempts_ck CHECK (attempts BETWEEN 0 AND 3),
    CONSTRAINT assistant_turns_running_ck CHECK (status <> 'running' OR started_at IS NOT NULL),
    CONSTRAINT assistant_turns_done_ck CHECK (status <> 'done' OR answered_through IS NOT NULL)
);
-- One turn per customer message.
CREATE UNIQUE INDEX assistant_turns_trigger_uk ON assistant_turns (trigger_message_id);
-- A chat's turns run one at a time.
CREATE UNIQUE INDEX assistant_turns_running_uk ON assistant_turns (conversation_id) WHERE status = 'running';
CREATE INDEX assistant_turns_due_idx ON assistant_turns (run_after) WHERE status IN ('pending', 'running');
CREATE INDEX assistant_turns_conversation_idx ON assistant_turns (conversation_id, status, finished_at);
