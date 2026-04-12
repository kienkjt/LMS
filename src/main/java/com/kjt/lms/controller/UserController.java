package com.kjt.lms.controller;

import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.response.APIResponse;
import com.kjt.lms.model.request.profile.ChangePasswordRequest;
import com.kjt.lms.model.request.profile.UpdateProfileRequest;
import com.kjt.lms.model.response.user.ProfileResponse;
import com.kjt.lms.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final MessageProvider messageProvider;

    @PostMapping("/change-password")
    public APIResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return APIResponse.success(null, messageProvider.getMessage("exception.passwordChanged"));
    }

    @GetMapping("/profile")
    public APIResponse<ProfileResponse> getProfile() {
        ProfileResponse response = userService.getProfile();
        return APIResponse.success(response, null);
    }

    @PutMapping("/profile")
    public APIResponse<ProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        ProfileResponse response = userService.updateProfile(request);
        return APIResponse.success(response, messageProvider.getMessage("profile.updated.success"));
    }

    @PostMapping("/avatar")
    public APIResponse<ProfileResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        ProfileResponse response = userService.uploadAvatar(file);
        return APIResponse.success(response, messageProvider.getMessage("profile.avatar.upload.success"));
    }

    @DeleteMapping("/avatar")
    public APIResponse<ProfileResponse> deleteAvatar() {
        ProfileResponse response = userService.deleteAvatar();
        return APIResponse.success(response, messageProvider.getMessage("profile.avatar.deleted.success"));
    }
}
