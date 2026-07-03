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
     * 유저가 개인 정보 제공 동의를 안했을 경우 null 체크하여 NPE 문제를 발생시키지 않습니다.
     * email이 null인 경우 name에 '구글 사용자'로 삽입됩니다.
     * email이 null이 아닌 경우 '@' 앞부분의 이메일 주소가 삽입됩니다.
     */
    private static OAuth2Attributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        String name = attributes != null ? (String) attributes.get("name") : null;
        String email = attributes != null ? (String) attributes.get("email") : null;

        if (name == null || name.isBlank()) {
            if (email != null && !email.isBlank() && email.contains("@")) {
                name = email.split("@")[0];
            } else {
                name = "구글 사용자";
            }
        }

        return new OAuth2Attributes(
                attributes,
                userNameAttributeName,
                name,
                email,
                attributes != null ? (String) attributes.get("picture") : null
        );
    }

    /**
     * 카카오 OAuth2 프로필 응답 정보를 파싱합니다.
     * 유저가 개인 정보 제공 동의를 안했을 경우 null 체크하여 NPE 문제를 발생시키지 않습니다.
     * email이 null인 경우 name에 '카카오 사용자'로 삽입됩니다.
     * email이 null이 아닌 경우 '@' 앞부분의 이메일 주소가 삽입됩니다.
     */
    @SuppressWarnings("unchecked")
    private static OAuth2Attributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = attributes != null ? (Map<String, Object>) attributes.get("kakao_account") : null;
        Map<String, Object> profile = kakaoAccount != null ? (Map<String, Object>) kakaoAccount.get("profile") : null;

        String nickname = profile != null ? (String) profile.get("nickname") : null;
        String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;

        if (nickname == null || nickname.isBlank()) {
            if (email != null && !email.isBlank() && email.contains("@")) {
                nickname = email.split("@")[0];
            } else {
                nickname = "카카오 사용자";
            }
        }

        return new OAuth2Attributes(
                attributes,
                userNameAttributeName,
                nickname,
                email,
                profile != null ? (String) profile.get("thumbnail_image_url") : null
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
