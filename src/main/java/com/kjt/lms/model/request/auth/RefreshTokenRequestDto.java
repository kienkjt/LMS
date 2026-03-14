package com.kjt.lms.model.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RefreshTokenRequestDto {
    @NotBlank(message = "validation.refreshToken.notBlank")
    @Pattern(regexp = "^[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_.+/=]*$", message = "validation.refreshToken.invalid")
    private String refreshToken;
}
