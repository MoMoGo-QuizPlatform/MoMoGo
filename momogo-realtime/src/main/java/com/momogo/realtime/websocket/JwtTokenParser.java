package com.momogo.realtime.websocket;

import com.momogo.core.common.exception.AuthErrorCode;
import com.momogo.core.common.exception.BusinessException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenParser {

  private final JWSVerifier accessTokenVerifier;
  private static final String ACCESS_TOKEN_SECRET_KEY = "${jwt.access-token.secret}";

  // 검증, 파싱이 완료된 토큰의 데이터를 담을 레코드 클래스
  public record TokenPrincipal(UUID userId, List<String> roles) {}

  public JwtTokenParser(@Value(ACCESS_TOKEN_SECRET_KEY) String accessTokenSecret) throws Exception {
    byte[] accessSecretBytes = accessTokenSecret.getBytes(StandardCharsets.UTF_8);
    this.accessTokenVerifier = new MACVerifier(accessSecretBytes);
  }

  /**
   * JWT 토큰을 1회 파싱하고, 서명 및 만료 시각, 타입 검증 후, TokenPrincipal 객체 반환
   * @param token JWT 토큰 문자
   * @return 검증된 유저의 식별자(UUID) 및 권한(roles) 정보 객체
   */
  public TokenPrincipal parseAndValidate(String token) {

    try {
      SignedJWT signedJWT = SignedJWT.parse(token);

      // 서명 검증
      if (!signedJWT.verify(accessTokenVerifier)) {
        throw new BusinessException(AuthErrorCode.SIGNATURE_FAILED);
      }

      JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

      // 만료 시각 검증
      Date exp = claimsSet.getExpirationTime();
      if (exp == null || exp.before(new Date())) {
        throw new BusinessException(AuthErrorCode.EXPIRED_TOKEN);
      }

      // 토큰 타입 검증 (access 토큰이 맞는지 확인)
      String tokenType = (String) claimsSet.getClaim("type");
      if (!"access".equals(tokenType)) {
        throw new BusinessException(AuthErrorCode.INVALID_TOKEN_TYPE);
      }

      // 필수 클레임 검증 (userId)
      Object userId = claimsSet.getClaim("userId");
      if (userId == null) {
        throw new BusinessException(AuthErrorCode.MISSING_TOKEN_CLAIM);
      }

      // 필수 클레임 검증 (roles), null 방어
      List<String> roles = claimsSet.getStringListClaim("roles");
      if (roles == null) {
        throw new BusinessException(AuthErrorCode.MISSING_TOKEN_CLAIM);
      }

      return new TokenPrincipal(UUID.fromString(userId.toString()), roles);

    } catch (BusinessException e){
        throw e;
    } catch (Exception e) {
      throw new BusinessException(AuthErrorCode.MALFORMED_TOKEN);
    }
  }


}
