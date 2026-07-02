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
import org.springframework.dao.DataIntegrityViolationException;
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

    /**
     * Super Admin의 경우 무조건 한명만 존재하므로 2단계 검증을 거쳐 Super Admin을 생성한다.
     * 1단계: Super Admin의 역할을 가진 유저가 존재하는지 확인한다.
     * 2단계: 해당 이메일을 가진 유저가 존재하는지 확인한다.
     *
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!userRepository.existsByRole(UserRole.SUPER_ADMIN)) {
            if (userRepository.existsByEmail(email)) {
                log.error("[SuperAdminInitializer] Super Admin 생성 예정 이메일이 이미 선점되어 생성을 생략합니다.");
                return;
            }
            log.info("[SuperAdminInitializer] Super Admin이 존재하지 않아 생성합니다.");
            User superAdmin = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .name(name)
                    .role(UserRole.SUPER_ADMIN)
                    .social(SocialType.NONE)
                    .build();
            try {
                userRepository.save(superAdmin);
                log.info("[SuperAdminInitializer] Super Admin 계정이 성공적으로 생성되었습니다.");
            } catch (DataIntegrityViolationException e) {
                log.info("[SuperAdminInitializer] Super Admin 계정은 이미 다른 인스턴스에서 생성하여 스킵합니다.");
            }
        } else {
            log.info("[SuperAdminInitializer] Super Admin 계정이 이미 존재하여 스킵합니다.");
        }
    }
}
