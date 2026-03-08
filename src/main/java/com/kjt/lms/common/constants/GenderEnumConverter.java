package com.kjt.lms.common.constants;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class GenderEnumConverter implements AttributeConverter<GenderEnum, String> {

    @Override
    public String convertToDatabaseColumn(GenderEnum attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public GenderEnum convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return GenderEnum.fromValue(dbData);
    }
}
