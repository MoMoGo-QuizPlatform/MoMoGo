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

import com.momogo.api.auth.util.EmailUtils;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2UserDetailsService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 소셜(구글/카카오) 인증 완료 후 사용자 조회 및 가입 처리
     *
     * 소셜 플랫폼의 인증이 성공하면 호출되어 다음 핵심 비즈니스 로직을 수행합니다:
     *
     * - 소셜 정보 획득: super.loadUser()를 통해 프로필 원본 정보를 가져온 후 구글/카카오 맞춤형으로 이메일, 닉네임, 프로필 이미지 URL을 파싱합니다.
     * - 이메일 누락 방어: 사용자가 동의를 거부하는 등의 원인으로 이메일이 없을 시 즉시 예외(email_required)를 던져 안전하게 차단합니다.
     * - 이메일 중복 및 연동 거부 (Strict Unique Email):
     *   이미 DB에 동일한 이메일로 가입된 유저가 있다면, 기존의 가입 방식(일반 NONE, 구글 GOOGLE, 카카오 KAKAO)과 현재 로그인하려는 방식이 일치하는지 검증합니다.
     *   가입 방식이 다를 경우(일반 회원이 소셜로 전환하려는 경우 포함) 중복 저장을 차단하고 OAuth2AuthenticationException(social_type_mismatch) 예외를 던집니다.
     * - 프로필 덮어쓰기 배제 (No Sync): 사용자가 서비스 내에서 직접 수정할 수 있는 프로필 데이터(이름, 사진)를 소셜 정보로 강제 리셋하지 않도록 로그인 시 정보 갱신(save)을 일절 배제합니다.
     * - 신규 가입: 이메일이 중복되지 않은 신규 소셜 유저는 임의의 난수 비밀번호를 인코딩하여 회원 가입을 자동으로 처리합니다.
     *
     * @param userRequest OAuth2 클라이언트 및 토큰 요청 정보
     * @return 스프링 시큐리티 컨텍스트에 바인딩할 사용자 인증 상세 객체 (MoMoGoUserDetails)
     * @throws OAuth2AuthenticationException 이메일 누락 또는 로그인 수단 불일치(중복) 시 발생
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
                new UserResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getName(),
                        user.getProfileImageUrl(),
                        user.getRole(),
                        user.getSocial(),
                        user.getIsBanned(),
                        user.getCreatedAt()
                ),
                user.getPassword(),
                attributes.attributes()
        );
    }

    private User validateSocialUser(User user, SocialType socialType) {
        // 소셜 로그인 시도 유저가 탈퇴 진행 중인 상태인 경우 자동으로 복구 처리
        if (user.getDeletedAt() != null) {
            if (!user.isRestorable()) {
                log.info("[OAuth2UserDetailsService] 복구 기간이 만료된 소셜 유저({}) 로그인 시도", EmailUtils.maskEmail(user.getEmail()));
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("restore_expired"),
                        "탈퇴 후 30일이 경과하여 복구할 수 없습니다."
                );
            }
            log.info("[OAuth2UserDetailsService] 탈퇴 대기 중인 소셜 유저({}) 복구 및 로그인 진행", EmailUtils.maskEmail(user.getEmail()));
            user.restore();
            userRepository.save(user);
        }

        if (user.getSocial() != socialType) {
            log.error("[OAuth2UserDetailsService] 이메일({})은 이미 {} 방식으로 가입되어 있어 {} 로그인하실 수 없습니다.",
                    EmailUtils.maskEmail(user.getEmail()), user.getSocial(), socialType);
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
