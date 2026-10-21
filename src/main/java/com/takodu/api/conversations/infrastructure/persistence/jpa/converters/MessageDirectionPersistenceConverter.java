package com.takodu.api.conversations.infrastructure.persistence.jpa.converters;

import com.takodu.api.conversations.domain.model.valueobjects.MessageDirection;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

/** Stored as lower-case text, as the {@code CHECK} constraint expects. */
@Converter
public class MessageDirectionPersistenceConverter implements AttributeConverter<MessageDirection, String> {
    @Override
    public String convertToDatabaseColumn(MessageDirection attribute) { return attribute == null ? null : attribute.name().toLowerCase(Locale.ROOT); }

    @Override
    public MessageDirection convertToEntityAttribute(String column) { return column == null ? null : MessageDirection.valueOf(column.toUpperCase(Locale.ROOT)); }
}
