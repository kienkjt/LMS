package com.kjt.lms.service.impl;

import com.kjt.lms.model.entity.RoleEntity;
import com.kjt.lms.model.entity.UserEntity;
import com.kjt.lms.repository.RoleRepository;
import com.kjt.lms.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    public UserDetailsServiceImpl(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + email));

        RoleEntity role = roleRepository.findById(user.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role not found for user: " + email));

        // Validate and normalize role code
        String roleCode = role.getCode();
        if (roleCode == null || roleCode.trim().isEmpty()) {
            throw new RuntimeException("Role code is null or empty for user: " + email);
        }

        // Trim and convert to uppercase
        roleCode = roleCode.trim().toUpperCase();

        // Ensure ROLE_ prefix is present exactly once
        String authority;
        if (roleCode.startsWith("ROLE_")) {
            authority = roleCode;
        } else {
            authority = "ROLE_" + roleCode;
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(Collections.singletonList(
                        new SimpleGrantedAuthority(authority)
                ))
                .build();
    }
}

