-- W11-W13: a template message keeps the parameters Meta fills in, so the sender job can send it as written.
ALTER TABLE messages ADD COLUMN template_parameters text[];
ALTER TABLE messages ADD CONSTRAINT messages_template_parameters_ck
    CHECK (kind <> 'template' OR template_parameters IS NOT NULL);
