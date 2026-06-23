package com.momogo.api.auth.details;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // TODO: 데이터베이스에서 유저 조회 및 UserDetails 구현체 매핑 로직 구현 예정
        throw new UsernameNotFoundException("User not found: " + username);
    }
}
