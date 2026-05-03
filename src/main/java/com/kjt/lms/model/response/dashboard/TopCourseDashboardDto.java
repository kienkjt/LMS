package com.kjt.lms.model.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class TopCourseDashboardDto {

    private UUID courseId;
    private String courseTitle;
    private long soldItems;
    private BigDecimal revenue;
    private Double avgRating;
    private Integer totalStudents;
}
