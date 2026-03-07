package com.kjt.lms.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {

        // tạm thời hardcode
        return Optional.of("SYSTEM");

        // Sau này có Spring Security sẽ lấy từ SecurityContext
    }
}
