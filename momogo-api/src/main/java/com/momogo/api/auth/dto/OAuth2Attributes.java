package com.momogo.api.auth.dto;

import com.momogo.core.domain.user.entity.User;
import com.momogo.core.domain.user.entity.enums.SocialType;
import com.momogo.core.domain.user.entity.enums.UserRole;

import java.util.Map;

/**
 * OAuth2Attributes
 * 소셜 플랫폼(구글/카카오)별 프로필 응답 속성값들을 일관된 규격으로 파싱하고
 * 신규 회원가입을 위한 엔티티(User)로 변환을 지원하는 DTO 클래스입니다.
 */
public record OAuth2Attributes(
        Map<String, Object> attributes,
        String nameAttributeKey,
        String name,
        String email,
        String profileImageUrl
) {
    /**
     * 플랫폼 구분(google/kakao)에 맞춰 적절한 팩토리 메서드를 호출하여 속성값을 파싱합니다.
     */
    public static OAuth2Attributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if ("kakao".equals(registrationId)) {
            return ofKakao(userNameAttributeName, attributes);
        }
        return ofGoogle(userNameAttributeName, attributes);
    }

    /**
     * 구글 OAuth2 프로필 응답 정보를 파싱합니다.
     */
    private static OAuth2Attributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return new OAuth2Attributes(
                attributes,
                userNameAttributeName,
                (String) attributes.get("name"),
                (String) attributes.get("email"),
                (String) attributes.get("picture")
        );
    }

    /**
     * 카카오 OAuth2 프로필 응답 정보를 파싱합니다.
     */
    @SuppressWarnings("unchecked")
    private static OAuth2Attributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        return new OAuth2Attributes(
                attributes,
                userNameAttributeName,
                (String) profile.get("nickname"),
                (String) kakaoAccount.get("email"),
                (String) profile.get("thumbnail_image_url")
        );
    }

    /**
     * 최초 소셜 가입 시 신규 회원 엔티티(User)를 빌드합니다.
     */
    public User toEntity(String tempPassword, SocialType socialType) {
        return User.builder()
                .name(name)
                .email(email)
                .password(tempPassword)
                .profileImageUrl(profileImageUrl)
                .role(UserRole.USER)
                .social(socialType)
                .build();
    }

}
