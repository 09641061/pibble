package com.takodu.api.organization.infrastructure.persistence.jpa.converters;

import com.takodu.api.shared.domain.model.valueobjects.Role;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

/** Stored as lower-case text, as the {@code CHECK} constraint expects. */
@Converter
public class RolePersistenceConverter implements AttributeConverter<Role, String> {
    @Override
    public String convertToDatabaseColumn(Role attribute) { return attribute == null ? null : attribute.name().toLowerCase(Locale.ROOT); }

    @Override
    public Role convertToEntityAttribute(String column) { return column == null ? null : Role.valueOf(column.toUpperCase(Locale.ROOT)); }
}
