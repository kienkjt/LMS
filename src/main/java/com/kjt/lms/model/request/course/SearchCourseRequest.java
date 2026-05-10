package com.kjt.lms.model.request.course;

import com.kjt.lms.common.constants.CommonStatusEnum;
import com.kjt.lms.common.constants.CourseLevelEnum;
import com.kjt.lms.common.constants.CourseStatusEnum;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchCourseRequest {
    private String keyword;
    private CommonStatusEnum active;
    private CourseStatusEnum courseStatus;
    private CourseLevelEnum courseLevel;
    private UUID categoryId;
    private BigDecimal priceMin;
    private BigDecimal priceMax;
}
