-- Meta's review of each notification template on a line's WhatsApp account (W11-W13). Signup creates the templates;
-- the message_template_status_update webhook keeps the status current. Notifications wait until it is approved.
CREATE TABLE whatsapp_line_templates (
    line_id  uuid NOT NULL REFERENCES whatsapp_lines (id) ON DELETE CASCADE,
    template text NOT NULL,
    status   text NOT NULL,
    CONSTRAINT whatsapp_line_templates_pk PRIMARY KEY (line_id, template),
    CONSTRAINT whatsapp_line_templates_template_ck CHECK (template IN ('reminder', 'confirmation', 'cancellation')),
    CONSTRAINT whatsapp_line_templates_status_ck
        CHECK (status IN ('pending', 'approved', 'rejected', 'paused', 'disabled'))
);
