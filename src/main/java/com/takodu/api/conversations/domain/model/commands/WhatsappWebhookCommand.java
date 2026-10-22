package com.takodu.api.conversations.domain.model.commands;

/** One fact a Meta webhook reports, already parsed into Takodu's terms. A delivery may carry several. */
public sealed interface WhatsappWebhookCommand permits RecordInboundMessageCommand, RecordBusinessAppEchoCommand,
        RecordHistoryMessageCommand, RecordDeliveryStatusCommand, DisconnectAccountLinesCommand, RecordTemplateStatusCommand { }
