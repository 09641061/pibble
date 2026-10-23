package com.takodu.api.conversations.application.internal.outboundservices.whatsapp;

import com.takodu.api.conversations.domain.model.valueobjects.EncryptedToken;
import com.takodu.api.conversations.domain.model.valueobjects.NotificationTemplate;
import com.takodu.api.conversations.domain.model.valueobjects.TemplateStatus;

import java.util.Map;

/**
 * @param displayPhone the number as Meta displays it, such as {@code +51 987 654 321}
 * @param accessToken the business token, already encrypted
 * @param templates Meta's review of each notification template on the account; one that could not be created is
 *        missing, and the notifications that need it wait
 */
public record OnboardedNumber(String displayPhone, EncryptedToken accessToken,
                              Map<NotificationTemplate, TemplateStatus> templates) { }
