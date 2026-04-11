package com.kjt.lms.model.request.profile;

import com.kjt.lms.common.constants.GenderEnum;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(min = 2, max = 100, message = "{validation.profile.fullName.size}")
    private String fullName;

    private GenderEnum gender;

    @Pattern(regexp = "^(\\+?[0-9]{9,15})?$", message = "{validation.profile.phoneNumber.invalid}")
    private String phoneNumber;

    @Size(max = 2000, message = "{validation.profile.bio.size}")
    private String bio;
}

