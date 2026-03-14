package com.kjt.lms.common.constants;


import com.kjt.lms.common.base.BaseEnum;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public abstract class GenericEnumConverter<E extends Enum<E> & BaseEnum> implements AttributeConverter<E, String> {

    private final Class<E> enumClass;

    protected GenericEnumConverter(Class<E> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public String convertToDatabaseColumn(E attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public E convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        for (E enumConstant : enumClass.getEnumConstants()) {
            if (enumConstant.getValue().equals(dbData)) {
                return enumConstant;
            }
        }

        throw new IllegalArgumentException("Unknown value: " + dbData + " for enum " + enumClass.getSimpleName());
    }
}
