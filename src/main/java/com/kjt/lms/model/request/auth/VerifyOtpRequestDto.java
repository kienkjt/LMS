package com.kjt.lms.model.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyOtpRequestDto {

    @NotBlank(message = "validation.email.notBlank")
    @Email(message = "validation.email.invalid")
    private String email;

    @NotBlank(message = "validation.otp.notBlank")
    @Size(min = 6, max = 6, message = "validation.otp.size")
    @Pattern(regexp = "\\d{6}", message = "validation.otp.pattern")
    private String otp;
}
