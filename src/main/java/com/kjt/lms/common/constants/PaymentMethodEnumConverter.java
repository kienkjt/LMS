package com.kjt.lms.common.constants;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PaymentMethodEnumConverter implements AttributeConverter<PaymentMethodEnum, String> {

    @Override
    public String convertToDatabaseColumn(PaymentMethodEnum attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public PaymentMethodEnum convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return PaymentMethodEnum.fromValue(dbData);
    }
}