package com.kjt.lms.model.request.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectCourseRequest {

    @NotBlank(message = "{validation.rejectReason.notBlank}")
    @Size(min = 10, max = 500, message = "{validation.rejectReason.size}")
    private String reason;
}

