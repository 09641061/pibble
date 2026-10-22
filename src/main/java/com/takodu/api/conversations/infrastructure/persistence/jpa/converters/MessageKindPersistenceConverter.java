package com.takodu.api.conversations.infrastructure.persistence.jpa.converters;

import com.takodu.api.conversations.domain.model.valueobjects.MessageKind;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

/** Stored as lower-case text, as the {@code CHECK} constraint expects. */
@Converter
public class MessageKindPersistenceConverter implements AttributeConverter<MessageKind, String> {
    @Override
    public String convertToDatabaseColumn(MessageKind attribute) { return attribute == null ? null : attribute.name().toLowerCase(Locale.ROOT); }

    @Override
    public MessageKind convertToEntityAttribute(String column) { return column == null ? null : MessageKind.valueOf(column.toUpperCase(Locale.ROOT)); }
}
