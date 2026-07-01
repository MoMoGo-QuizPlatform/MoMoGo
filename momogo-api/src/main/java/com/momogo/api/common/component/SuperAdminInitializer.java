package com.momogo.api.common.component;

import com.momogo.core.domain.user.entity.User;
import com.momogo.core.domain.user.entity.enums.SocialType;
import com.momogo.core.domain.user.entity.enums.UserRole;
import com.momogo.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Super Admin 생성 컴포넌트
 * 서버 구동시 DB에 Super Admin이 존재하지 않을 경우 생성된다.
 * 프로퍼티로 Super Admin 계정의 정보를 보호하여 주입한다.
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.super-admin.email}")
    private String email;

    @Value("${app.super-admin.password}")
    private String password;

    @Value("${app.super-admin.name}")
    private String name;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!userRepository.existsByEmail(email)) {
            User superAdmin = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .name(name)
                    .role(UserRole.SUPER_ADMIN)
                    .social(SocialType.NONE)
                    .build();
            userRepository.save(superAdmin);
            log.info("Super Admin 계정이 성공적으로 생성되었습니다.");
        } else {
            log.info("Super Admin 계정이 이미 존재하여 스킵합니다.");
        }
    }
}
