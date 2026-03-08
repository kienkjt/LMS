package com.kjt.lms.common.constants;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class LessonTypeEnumConverter implements AttributeConverter<LessonTypeEnum, String> {

    @Override
    public String convertToDatabaseColumn(LessonTypeEnum attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public LessonTypeEnum convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return LessonTypeEnum.fromValue(dbData);
    }
}