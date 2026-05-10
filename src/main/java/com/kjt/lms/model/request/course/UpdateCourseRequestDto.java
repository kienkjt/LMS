package com.kjt.lms.model.request.course;

import com.kjt.lms.common.constants.CourseLevelEnum;
import jakarta.validation.constraints.*;
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
public class UpdateCourseRequestDto {

    @NotBlank(message = "{validation.title.notBlank}")
    @Size(min = 5, max = 200, message = "{validation.title.size}")
    private String title;

    @NotBlank(message = "{validation.shortDescription.notBlank}")
    @Size(min = 10, max = 1000, message = "{validation.shortDescription.size}")
    private String shortDescription;

    @Size(max = 5000, message = "{validation.fullDescription.size}")
    private String fullDescription;

    @NotNull(message = "{validation.price.notNull}")
    @DecimalMin(value = "0.0", message = "{validation.price.min}")
    @DecimalMax(value = "999999999.99", message = "{validation.price.max}")
    private BigDecimal price;

    @DecimalMin(value = "0.0", message = "{validation.discountPrice.min}")
    @DecimalMax(value = "999999999.99", message = "{validation.discountPrice.max}")
    private BigDecimal discountPrice;

    @NotNull(message = "{validation.level.notNull}")
    private CourseLevelEnum level;

    private Integer totalDuration;

    @Size(max = 50, message = "{validation.language.size}")
    private String language;

    @Size(max = 100, message = "{validation.certificate.size}")
    private String certificate;

    @Size(max = 2000, message = "{validation.requirements.size}")
    private String requirements;

    @Size(max = 2000, message = "{validation.whatYouWillLearn.size}")
    private String whatYouWillLearn;

    private UUID categoryId;
}
