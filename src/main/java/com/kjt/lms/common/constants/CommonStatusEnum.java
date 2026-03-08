package com.kjt.lms.common.constants;

import lombok.Generated;

public enum CommonStatusEnum {
    ACTIVE(1, "Đang hoạt động"),
    INACTIVE(0, "Dừng hoạt động"),
    DELETED(-1, "Đã xóa");

    private final int value;
    private final String description;

    public static CommonStatusEnum fromValue(int value) {
        for(CommonStatusEnum status : values()) {
            if (status.value == value) {
                return status;
            }
        }

        throw new IllegalArgumentException("Giá trị không hợp lệ cho CommonStatusEnum: " + value);
    }

    @Generated
    public int getValue() {
        return this.value;
    }

    @Generated
    public String getDescription() {
        return this.description;
    }

    @Generated
    private CommonStatusEnum(final int value, final String description) {
        this.value = value;
        this.description = description;
    }
}
