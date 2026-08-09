package com.momogo.api.auth.details;

import com.momogo.core.domain.user.dto.response.UserResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class MoMoGoUserDetails implements UserDetails, OAuth2User {

    private final UserResponse userResponse;
    private final String password;
    private final Map<String, Object> attributes;

    public MoMoGoUserDetails(UserResponse userResponse, String password) {
        this(userResponse, password, Collections.emptyMap());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + userResponse.role().name())
        );
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return userResponse.email();
    }

    @Override
    public String getName() {
        return userResponse.name();
    }

    @Override
    public boolean isAccountNonLocked() {
        // 계정 정지(밴) 상태인 경우에만 잠금(Locked) 처리.
        return !Boolean.TRUE.equals(userResponse.isBanned());
    }

    @Override
    public boolean isEnabled() {
        // 탈퇴(논리 삭제) 진행 중이거나 완료된 계정인 경우 비활성화(Disabled) 처리
        return userResponse.deletedAt() == null;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}