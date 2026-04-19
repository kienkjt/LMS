package com.kjt.lms.model.request.cart;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AddToCartRequestDto {
    @NotNull(message = "Course ID cannot be null")
    private UUID courseId;
}
