package com.momogo.api.auth.details;

import com.momogo.core.domain.user.dto.response.UserResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class MoMoGoUserDetails implements UserDetails {

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

    public String getName() {
        return userResponse.name();
    }
}
