package com.kjt.lms.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Role {

    GUEST("1", "Khách"),
    STUDENT("2", "Học viên"),
    INSTRUCTOR("3", "Giảng viên"),
    ADMIN("4", "Quản trị viên");

    private final String value;
    private final String description;

    public static Role fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (Role type : values()) {
            if (type.value.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid Role value: " + value);
    }
}