package com.kjt.lms.common.constants;

import com.kjt.lms.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LessonTypeEnum implements BaseEnum {

    VIDEO("1", "Bài học video"),
    DOCUMENT("2", "Tài liệu"),
    QUIZ("3", "Trắc nghiệm"),
    CODING("4", "Bài tập lập trình"),
    TEXT("5", "Nội dung văn bản");

    private final String value;
    private final String description;
}