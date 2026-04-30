package com.kjt.lms;

import com.kjt.lms.repository.OtpRedisRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableJpaRepositories(
        basePackages = "com.kjt.lms.repository",
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = OtpRedisRepository.class)
)
@EnableRedisRepositories(
        basePackageClasses = OtpRedisRepository.class,
        includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = OtpRedisRepository.class)
)
public class LmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(LmsApplication.class, args);
    }

}
