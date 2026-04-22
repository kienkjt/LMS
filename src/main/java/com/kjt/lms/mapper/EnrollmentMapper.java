package com.kjt.lms.mapper;

import com.kjt.lms.model.entity.EnrollmentEntity;
import com.kjt.lms.model.response.enrollment.EnrollmentResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface EnrollmentMapper {

    @Mapping(target = "enrollmentId", source = "id")
    @Mapping(target = "enrolledAt", source = "createdAt")
    EnrollmentResponseDto toResponse(EnrollmentEntity enrollment);

    default EnrollmentEntity toCreateEntity(UUID studentId, UUID courseId) {
        return EnrollmentEntity.builder()
                .studentId(studentId)
                .courseId(courseId)
                .progressPercent(BigDecimal.ZERO)
                .build();
    }
}

