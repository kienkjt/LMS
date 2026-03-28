package com.kjt.lms.service.impl;

import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.model.entity.UserEntity;
import com.kjt.lms.model.request.profile.ChangePasswordRequest;
import com.kjt.lms.repository.UserRepository;
import com.kjt.lms.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final MessageProvider messageProvider;

    @Override
    public void changePassword(ChangePasswordRequest request) {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(messageProvider.getMessage("exception.user.notfound")));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException(messageProvider.getMessage("exception.user.invalid.current.password"));
        }

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BusinessException(messageProvider.getMessage("exception.user.passwords.do.not.match"));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);

    }
}
