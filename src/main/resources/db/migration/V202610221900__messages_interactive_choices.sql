-- 08 §5: the assistant's reply buttons or list rows stay on the outbox row, so the sender job sends them as chosen.
ALTER TABLE messages
    ADD COLUMN interactive_style   text,
    ADD COLUMN interactive_options text[],
    ADD CONSTRAINT messages_interactive_style_ck CHECK (interactive_style IN ('buttons', 'list')),
    ADD CONSTRAINT messages_interactive_choices_ck
        CHECK ((interactive_style IS NULL) = (interactive_options IS NULL)
            AND (interactive_style IS NULL OR (direction = 'out' AND kind = 'interactive')));
