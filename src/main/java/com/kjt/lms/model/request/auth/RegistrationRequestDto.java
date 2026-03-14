package com.kjt.lms.model.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistrationRequestDto {
    @NotBlank(message = "{validation.email.notBlank}")
    @Email(message = "{validation.email.invalid}")
    private String email;

    @NotBlank(message = "{validation.password.notBlank}")
    @Size(min = 8, message = "{validation.password.size}")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
             message = "{validation.password.pattern}")
    private String password;

    @NotBlank(message = "{validation.name.notBlank}")
    @Size(min = 2, message = "{validation.name.size}")
    private String fullName;

    @NotBlank(message = "{validation.role.notBlank}")
    @Pattern(regexp = "^(STUDENT|INSTRUCTOR)$", message = "{validation.role.invalid}")
    private String role;
}
