package com.kjt.lms.common.annotation;

import lombok.Getter;

@Getter
public enum ColCellType { // xác định kiểu dữ liệu của cell trong excel
    _STRING("STRING"),
    _DATE("DATE"),
    _DOLLARS("DOLLARS"),
    _DOUBLE("DOUBLE"),
    _INTEGER("INTEGER"),
    _FORMULA("FORMULA"); // công thức excel

    private final String value;
    ColCellType(String value) {
        this.value = value;
    }

}
