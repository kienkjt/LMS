package com.kjt.lms.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented // cho phép annotation này được hiển thị trong tài liệu javadoc
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface ExcelColumn { // config thông tin cho từng cột trong excel
    int col() default 0;

    ColCellType type() default ColCellType._STRING;

    String title() default "";

    int width() default 2000;

    short alignHorizontal() default 1;

    short alignVertical() default 1;

    String format() default "";

    String style() default "LEFT";

    boolean isMergeCell() default false;

    int endIndexMerge() default 8;

    boolean isSubItem() default false;
}
