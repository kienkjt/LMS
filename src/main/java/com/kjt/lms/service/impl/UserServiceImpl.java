package com.kjt.lms.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.model.entity.UserEntity;
import com.kjt.lms.model.request.profile.ChangePasswordRequest;
import com.kjt.lms.model.request.profile.UpdateProfileRequest;
import com.kjt.lms.model.response.ProfileResponse;
import com.kjt.lms.repository.UserRepository;
import com.kjt.lms.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final MessageProvider messageProvider;
    private final Cloudinary cloudinary;

    @Value("${app.profile.avatar.max-size-bytes:5242880}")
    private long maxAvatarSizeBytes;

    @Value("${app.profile.avatar.allowed-content-types:image/jpeg,image/png,image/webp}")
    private String allowedAvatarContentTypes;

    @Override
    public void changePassword(ChangePasswordRequest request) {
        UserEntity user = getCurrentUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException(messageProvider.getMessage("exception.user.invalid.current.password"));
        }

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BusinessException(messageProvider.getMessage("exception.user.passwords.do.not.match"));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public ProfileResponse getProfile() {
        return toProfileResponse(getCurrentUser());
    }

    @Override
    public ProfileResponse updateProfile(UpdateProfileRequest request) {
        UserEntity user = getCurrentUser();

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio().trim());
        }

        UserEntity savedUser = userRepository.save(user);
        return toProfileResponse(savedUser);
    }

    @Override
    public ProfileResponse uploadAvatar(MultipartFile file) {
        validateAvatarFile(file);

        UserEntity user = getCurrentUser();

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "lms/avatars",
                            "resource_type", "image"
                    )
            );

            Object secureUrl = uploadResult.get("secure_url");
            if (secureUrl == null) {
                throw new BusinessException(messageProvider.getMessage("profile.avatar.upload.failed"));
            }

            user.setAvatar(secureUrl.toString());
            UserEntity savedUser = userRepository.save(user);
            return toProfileResponse(savedUser);
        } catch (IOException ex) {
            throw new BusinessException(messageProvider.getMessage("profile.avatar.upload.failed"));
        }
    }

    @Override
    public ProfileResponse deleteAvatar() {
        UserEntity user = getCurrentUser();
        user.setAvatar(null);
        UserEntity savedUser = userRepository.save(user);
        return toProfileResponse(savedUser);
    }

    private void validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(messageProvider.getMessage("validation.profile.avatar.required"));
        }

        if (file.getSize() > maxAvatarSizeBytes) {
            throw new BusinessException(messageProvider.getMessage("validation.profile.avatar.maxSize", maxAvatarSizeBytes));
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BusinessException(messageProvider.getMessage("validation.profile.avatar.invalidType"));
        }

        String normalizedContentType = contentType.toLowerCase(Locale.ROOT).trim();
        if (!getAllowedAvatarContentTypes().contains(normalizedContentType)) {
            throw new BusinessException(messageProvider.getMessage("validation.profile.avatar.invalidType"));
        }
    }

    private Set<String> getAllowedAvatarContentTypes() {
        return Arrays.stream(allowedAvatarContentTypes.split(","))
                .map(value -> value.toLowerCase(Locale.ROOT).trim())
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());
    }

    private UserEntity getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new BusinessException(messageProvider.getMessage("exception.user.notfound")));
    }

    private ProfileResponse toProfileResponse(UserEntity user) {
        return new ProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getGender(),
                user.getBio(),
                user.getPhoneNumber(),
                user.getAvatar()
        );
    }
}
