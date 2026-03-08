package com.kjt.lms.common.constants;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class NotificationTypeEnumConverter implements AttributeConverter<NotificationTypeEnum, String> {

    @Override
    public String convertToDatabaseColumn(NotificationTypeEnum attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public NotificationTypeEnum convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return NotificationTypeEnum.fromValue(dbData);
    }
}