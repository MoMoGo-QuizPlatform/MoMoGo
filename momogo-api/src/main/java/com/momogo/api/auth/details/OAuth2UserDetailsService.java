package com.momogo.api.auth.details;

import com.momogo.api.auth.dto.OAuth2Attributes;
import com.momogo.core.domain.user.dto.response.UserResponse;
import com.momogo.core.domain.user.entity.User;
import com.momogo.core.domain.user.entity.enums.SocialType;
import com.momogo.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.momogo.core.common.util.EmailFormatter;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2UserDetailsService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 소셜(구글/카카오) 인증이 완료된 후 사용자를 조회하거나 신규 가입을 처리합니다.
     *
     * @param userRequest OAuth2 클라이언트 및 토큰 요청 정보
     * @return 스프링 시큐리티 컨텍스트에 등록될 사용자 상세 객체
     * @throws OAuth2AuthenticationException 이메일 누락, 탈퇴 기간 만료, 정지(밴) 유저 혹은 기존 가입 수단 불일치 시 발생
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("[OAuth2UserDetailsService] loadUser 시작 - RegistrationId: {}",
                userRequest.getClientRegistration().getRegistrationId());
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuth2Attributes attributes = OAuth2Attributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        // 소셜 이메일 정보 누락 방어
        if (attributes.email() == null || attributes.email().isBlank()) {
            log.error("[OAuth2UserDetailsService] {} 플랫폼으로부터 이메일 정보를 획득하지 못했습니다.", registrationId);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_required"),
                    "이메일 정보 제공 동의가 필수적입니다."
            );
        }

        SocialType socialType = SocialType.valueOf(registrationId.toUpperCase());

        User user = userRepository.findByEmail(attributes.email())
                .map(existingUser -> validateSocialUser(existingUser, socialType))
                .orElseGet(() -> registerSocialUser(attributes, socialType));

        return new MoMoGoUserDetails(
                UserResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .profileImageUrl(user.getProfileImageUrl())
                        .role(user.getRole())
                        .social(user.getSocial())
                        .isBanned(user.getIsBanned())
                        .createdAt(user.getCreatedAt())
                        .deletedAt(user.getDeletedAt())
                        .build(),
                user.getPassword(),
                attributes.attributes()
        );
    }

    private User validateSocialUser(User user, SocialType socialType) {
        if (Boolean.TRUE.equals(user.getIsBanned())) {
            log.error("[OAuth2UserDetailsService] 밴 처리된 유저({}) 로그인 시도", EmailFormatter.mask(user.getEmail()));
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("user_banned"),
                    "정지된 계정입니다. 관리자에게 문의하세요."
            );
        }

        // 소셜 로그인 시도 유저가 탈퇴 진행 중인 상태인 경우 자동으로 복구 처리
        if (user.getDeletedAt() != null) {
            if (!user.isRestorable()) {
                log.info("[OAuth2UserDetailsService] 복구 기간이 만료된 소셜 유저({}) 로그인 시도", EmailFormatter.mask(user.getEmail()));
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("restore_expired"),
                        "탈퇴 후 30일이 경과하여 복구할 수 없습니다."
                );
            }
            log.info("[OAuth2UserDetailsService] 탈퇴 대기 중인 소셜 유저({}) 복구 및 로그인 진행", EmailFormatter.mask(user.getEmail()));
            user.restore();
            userRepository.save(user);
        }

        if (user.getSocial() != socialType) {
            log.error("[OAuth2UserDetailsService] 이메일({})은 이미 {} 방식으로 가입되어 있어 {} 로그인하실 수 없습니다.",
                    EmailFormatter.mask(user.getEmail()), user.getSocial(), socialType);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("social_type_mismatch"),
                    String.format("이미 %s 계정으로 가입된 이메일 주소입니다. 기존 로그인 방식을 이용해 주세요.",
                            user.getSocial() == SocialType.NONE ? "일반" : user.getSocial().name())
            );
        }
        return user;
    }

    private User registerSocialUser(OAuth2Attributes attributes, SocialType socialType) {
        String tempPassword = passwordEncoder.encode(UUID.randomUUID().toString());
        User newUser = attributes.toEntity(tempPassword, socialType);
        return userRepository.save(newUser);
    }
}
