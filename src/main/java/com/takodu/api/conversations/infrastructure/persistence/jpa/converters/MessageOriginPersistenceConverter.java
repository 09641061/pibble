package com.takodu.api.conversations.infrastructure.persistence.jpa.converters;

import com.takodu.api.conversations.domain.model.valueobjects.MessageOrigin;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

/** Stored as lower-case text, as the {@code CHECK} constraint expects. */
@Converter
public class MessageOriginPersistenceConverter implements AttributeConverter<MessageOrigin, String> {
    @Override
    public String convertToDatabaseColumn(MessageOrigin attribute) { return attribute == null ? null : attribute.name().toLowerCase(Locale.ROOT); }

    @Override
    public MessageOrigin convertToEntityAttribute(String column) { return column == null ? null : MessageOrigin.valueOf(column.toUpperCase(Locale.ROOT)); }
}
