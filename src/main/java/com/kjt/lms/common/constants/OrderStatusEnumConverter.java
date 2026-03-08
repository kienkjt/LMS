package com.kjt.lms.common.constants;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OrderStatusEnumConverter implements AttributeConverter<OrderStatusEnum, String> {

    @Override
    public String convertToDatabaseColumn(OrderStatusEnum attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public OrderStatusEnum convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return OrderStatusEnum.fromValue(dbData);
    }
}