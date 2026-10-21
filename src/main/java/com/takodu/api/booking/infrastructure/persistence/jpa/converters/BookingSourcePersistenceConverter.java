package com.takodu.api.booking.infrastructure.persistence.jpa.converters;

import com.takodu.api.booking.domain.model.valueobjects.BookingSource;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

/** Stored as lower-case text, as the {@code CHECK} constraint expects. */
@Converter
public class BookingSourcePersistenceConverter implements AttributeConverter<BookingSource, String> {
    @Override
    public String convertToDatabaseColumn(BookingSource attribute) { return attribute == null ? null : attribute.name().toLowerCase(Locale.ROOT); }

    @Override
    public BookingSource convertToEntityAttribute(String column) { return column == null ? null : BookingSource.valueOf(column.toUpperCase(Locale.ROOT)); }
}
