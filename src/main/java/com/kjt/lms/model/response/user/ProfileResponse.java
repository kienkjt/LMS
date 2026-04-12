package com.kjt.lms.model.response.user;

import com.kjt.lms.common.constants.GenderEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
    private UUID id;
    private String fullName;
    private String email;
    private GenderEnum gender;
    private String bio;
    private String phoneNumber;
    private String avatar;
}

