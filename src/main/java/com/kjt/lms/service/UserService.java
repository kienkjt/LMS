package com.kjt.lms.service;

import com.kjt.lms.model.request.profile.ChangePasswordRequest;

public interface UserService {
    void changePassword(ChangePasswordRequest request);

}
