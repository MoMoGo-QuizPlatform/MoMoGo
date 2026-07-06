package com.momogo.realtime.websocket;

import com.momogo.core.common.exception.AuthErrorCode;
import com.momogo.core.common.exception.BusinessException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

  private final JwtTokenParser jwtTokenParser;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {

    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
      String authHeader = accessor.getFirstNativeHeader("Authorization");
      log.info("[JwtChannelInterceptor] 웹소켓 STOMP 연결 시도");

      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7);
        try {
          UUID userId = jwtTokenParser.extractUserId(token);
          List<String> roles = jwtTokenParser.extractRoles(token);

          List<GrantedAuthority> authorities = roles.stream()
              .map(SimpleGrantedAuthority::new)
              .map(GrantedAuthority.class::cast)
              .toList();

          UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
              userId,
              null,
              authorities
          );
          accessor.setUser(authentication);
          log.info("[JwtChannelInterceptor] 웹소켓 인증 성공 - userId: {}, authorities: {}", userId, roles);
        } catch (BusinessException e) {
          log.error("[JwtChannelInterceptor] 웹소켓 비즈니스 예외 발생 - code: {}, message: {}",
              e.getErrorCode().getCode(), e.getMessage());

          throw new IllegalArgumentException(e.getErrorCode().getMessage());
        } catch (Exception e) {
          log.error("[JwtChannelInterceptor] 웹소켓 미정의 예외 발생", e);
          throw new IllegalArgumentException(AuthErrorCode.INVALID_TOKEN.getMessage());
        }
      } else {
        throw new IllegalArgumentException(AuthErrorCode.TOKEN_NOT_FOUND.getMessage());
      }
    }
    return message;
  }

}
