package com.momogo.api.auth.details;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class OAuth2UserDetailsService extends DefaultOAuth2UserService {
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // TODO: 소셜 로그인(Google, Kakao 등) 유저 정보 가공 및 DB 등록/조회 로직 구현 예정
        return super.loadUser(userRequest);
    }
}
