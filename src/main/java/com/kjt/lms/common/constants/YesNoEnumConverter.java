package com.kjt.lms.common.constants;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class YesNoEnumConverter implements AttributeConverter<YesNoEnum, Integer> {

    @Override
    public Integer convertToDatabaseColumn(YesNoEnum attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public YesNoEnum convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return YesNoEnum.fromValue(dbData);
    }
}
