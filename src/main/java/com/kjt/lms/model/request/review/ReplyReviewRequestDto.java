package com.kjt.lms.model.request.review;

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
public class ReplyReviewRequestDto {

    @NotBlank(message = "{validation.review.reply.notBlank}")
    @Size(max = 2000, message = "{validation.review.reply.size}")
    private String reply;
}
