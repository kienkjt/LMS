package com.kjt.lms.service.impl;

import com.kjt.lms.common.security.LmsUserPrincipal;
import com.kjt.lms.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserRepository.UserAuthProjection user = userRepository.findAuthByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Khong tim thay nguoi dung: " + email));

        String roleCode = user.getRoleCode();
        if (roleCode == null || roleCode.trim().isEmpty()) {
            throw new RuntimeException("Role code is null or empty for user: " + email);
        }

        roleCode = roleCode.trim().toUpperCase();

        String authority;
        if (roleCode.startsWith("ROLE_")) {
            authority = roleCode;
        } else {
            authority = "ROLE_" + roleCode;
        }

        return new LmsUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                Boolean.TRUE.equals(user.getVerified()),
                !Boolean.TRUE.equals(user.getLocked()),
                Collections.singletonList(new SimpleGrantedAuthority(authority))
        );
    }
}
