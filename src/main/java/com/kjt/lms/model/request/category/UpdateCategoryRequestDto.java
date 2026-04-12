package com.kjt.lms.model.request.category;

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
public class UpdateCategoryRequestDto {

    @NotBlank(message = "{validation.category.name.notBlank}")
    @Size(max = 100, message = "{validation.category.name.size}")
    private String name;

    @Size(max = 2000, message = "{validation.category.description.size}")
    private String description;
}

