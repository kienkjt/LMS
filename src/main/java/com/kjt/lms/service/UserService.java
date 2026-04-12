package com.kjt.lms.service;

import com.kjt.lms.model.request.profile.ChangePasswordRequest;
import com.kjt.lms.model.request.profile.UpdateProfileRequest;
import com.kjt.lms.model.response.user.ProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    void changePassword(ChangePasswordRequest request);

    ProfileResponse getProfile();

    ProfileResponse updateProfile(UpdateProfileRequest request);

    ProfileResponse uploadAvatar(MultipartFile file);

    ProfileResponse deleteAvatar();
}
