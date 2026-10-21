package com.takodu.api.conversations.application.internal.outboundservices.whatsapp;

import com.takodu.api.conversations.domain.model.valueobjects.EncryptedToken;

/**
 * @param displayPhone the number as Meta displays it, such as {@code +51 987 654 321}
 * @param accessToken the business token, already encrypted
 */
public record OnboardedNumber(String displayPhone, EncryptedToken accessToken) { }
