package com.kjt.lms.model.request.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReviewRequestDto {

    @NotNull(message = "{validation.review.rating.notNull}")
    @Min(value = 1, message = "{validation.review.rating.min}")
    @Max(value = 5, message = "{validation.review.rating.max}")
    private Integer rating;

    @Size(max = 2000, message = "{validation.review.comment.size}")
    private String comment;
}
