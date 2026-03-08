package com.kjt.lms.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum YesNoEnum {
    ACTIVE(1, "Có"),
    INACTIVE(0, "Không");

    private final int value;
    private final String description;

    public static YesNoEnum fromValue(int value) {
        for (YesNoEnum status : YesNoEnum.values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("Giá trị không hợp lệ cho YesNoEnum: " + value);
    }
}