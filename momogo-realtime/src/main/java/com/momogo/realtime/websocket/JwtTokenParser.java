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

  private static final String SECRET_KEY = "${jwt.access-token.secret}";

  public JwtTokenParser(@Value(SECRET_KEY) String accessTokenSecret) throws Exception {
    byte[] accessSecretBytes = accessTokenSecret.getBytes(StandardCharsets.UTF_8);
    this.accessTokenVerifier = new MACVerifier(accessSecretBytes);
  }

  /**
   * JWT 토큰의 서명 및 만료 시각 검증 후, 사용자 ID 반환
   * @param token JWT 토큰
   * @return 사용자 ID
   */
  public UUID extractUserId(String token) {

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

      // 3. 토큰 타입 검증 (access 토큰이 맞는지 확인)
      String tokenType = (String) claimsSet.getClaim("type");
      if (!"access".equals(tokenType)) {
        throw new BusinessException(AuthErrorCode.INVALID_TOKEN_TYPE);
      }

      Object userId = claimsSet.getClaim("userId");
      if (userId == null) {
        throw new BusinessException(AuthErrorCode.MISSING_TOKEN_CLAIM);
      }

      return UUID.fromString(userId.toString());
    } catch (BusinessException e){
        throw e;
    } catch (Exception e) {
      throw new BusinessException(AuthErrorCode.MALFORMED_TOKEN);
    }
  }

  /**
   * 토큰에서 권한 리스트를 추출해서 반환
   * @param token JWT 토큰
   * @return 권한 리스트
   */
  public List<String> extractRoles(String token) {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);
      JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

      return claimsSet.getStringListClaim("roles");
    } catch (Exception e) {
      throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
    }
  }
}
