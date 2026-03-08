package com.kjt.lms.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GenderEnum {

    MALE("1", "Nam"),
    FEMALE("2", "Nữ"),
    OTHER("3", "Khác");

    private final String value;
    private final String description;
        public static GenderEnum fromValue(String value) {
            if (value == null) {
                return null;
            }
            for (GenderEnum type : values()) {
                if (type.value.equalsIgnoreCase(value.trim())) {
                    return type;
                }
            }
            throw new IllegalArgumentException ("Invalid Gender value: " + value);
        }
}