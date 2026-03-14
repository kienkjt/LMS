package com.kjt.lms.model.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequestDto {
    @NotBlank(message = "validation.email.notBlank")
    @Email(message = "validation.email.invalid")
    private String email;

    @NotBlank(message = "validation.password.notBlank")
    @Size(min = 8, message = "validation.password.size")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "validation.password.pattern")
    private String password;
}
