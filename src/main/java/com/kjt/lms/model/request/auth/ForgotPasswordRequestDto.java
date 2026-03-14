package com.kjt.lms.model.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequestDto {
    @NotBlank(message = "validation.email.notBlank")
    @Email(message = "validation.email.invalid")
    private String email;
}
