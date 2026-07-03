package com.momogo.api.auth.jwt;

import com.momogo.api.auth.details.MoMoGoUserDetails;
import com.momogo.api.auth.exception.AuthErrorCode;
import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.domain.user.dto.response.UserResponse;
import com.momogo.core.domain.user.entity.enums.UserRole;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "REFRESH_TOKEN";
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    private final JWSSigner accessTokenSigner;
    private final JWSVerifier accessTokenVerifier;
    private final JWSSigner refreshTokenSigner;
    private final JWSVerifier refreshTokenVerifier;

    private final boolean refreshCookieSecure;
    private final String refreshCookieSameSite;

    public JwtTokenProvider(
            @Value("${jwt.access-token.secret}") String accessTokenSecret,
            @Value("${jwt.access-token.exp}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token.secret}") String refreshTokenSecret,
            @Value("${jwt.refresh-token.exp}") long refreshTokenExpirationMs,
            @Value("${jwt.refresh-token.cookie.secure}") boolean refreshCookieSecure,
            @Value("${jwt.refresh-token.cookie.same-site}") String refreshCookieSameSite
    ) throws JOSEException {
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.refreshCookieSecure = refreshCookieSecure;
        this.refreshCookieSameSite = refreshCookieSameSite;

        byte[] accessSecretBytes = accessTokenSecret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenSigner = new MACSigner(accessSecretBytes);
        this.accessTokenVerifier = new MACVerifier(accessSecretBytes);

        byte[] refreshSecretBytes = refreshTokenSecret.getBytes(StandardCharsets.UTF_8);
        this.refreshTokenSigner = new MACSigner(refreshSecretBytes);
        this.refreshTokenVerifier = new MACVerifier(refreshSecretBytes);
    }

    public String generateAccessToken(MoMoGoUserDetails userDetails) throws JOSEException {
        return createToken(userDetails, accessTokenExpirationMs, accessTokenSigner, "access");
    }

    public String generateRefreshToken(MoMoGoUserDetails userDetails) throws JOSEException {
        return createToken(userDetails, refreshTokenExpirationMs, refreshTokenSigner, "refresh");
    }

    private String createToken(
            MoMoGoUserDetails userDetails,
            long expirationMs,
            JWSSigner signer,
            String tokenType
    ) throws JOSEException {
        String tokenId = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(userDetails.getUsername())
                .jwtID(tokenId)
                .claim("userId", userDetails.getUserResponse().id())
                .claim("userEmail", userDetails.getUserResponse().email())
                .claim("name", userDetails.getUserResponse().name())
                .claim("type", tokenType)
                .claim("roles",
                        userDetails.getAuthorities()
                                .stream()
                                .map(GrantedAuthority::getAuthority)
                                .toList()
                )
                .issueTime(now)
                .expirationTime(expiryDate)
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    public ResponseCookie generateRefreshTokenCookie(String refreshToken) {
        return createCookie(refreshToken, refreshTokenExpirationMs / 1000);
    }

    public ResponseCookie generateRefreshTokenExpirationCookie() {
        return createCookie("", 0);
    }

    public void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = generateRefreshTokenCookie(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void expireRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = generateRefreshTokenExpirationCookie();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public boolean validateAccessToken(String token) {
        return verifyToken(token, accessTokenVerifier, "access");
    }

    public boolean validateRefreshToken(String token) {
        return verifyToken(token, refreshTokenVerifier, "refresh");
    }

    private boolean verifyToken(String token, JWSVerifier verifier, String expectedType) {
        try {
            SignedJWT signedJWT = parseToken(token);
            if (!signedJWT.verify(verifier)) {
                return false;
            }

            String tokenType = (String) signedJWT.getJWTClaimsSet().getClaim("type");
            if (!expectedType.equals(tokenType)) {
                return false;
            }

            Date exp = signedJWT.getJWTClaimsSet().getExpirationTime();
            return exp != null && exp.after(new Date());

        } catch (Exception e) {
            return false;
        }
    }

    public String getUserEmailFromToken(String token) {
        try {
            return parseToken(token).getJWTClaimsSet().getSubject();
        } catch (Exception e) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    public String getTokenId(String token) {
        try {
            return parseToken(token).getJWTClaimsSet().getJWTID();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    public Date getExpiration(String token) {
        try {
            return parseToken(token).getJWTClaimsSet().getExpirationTime();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    public UUID getUserIdFromTokenWithoutExpiration(String token, String expectedType) {
        try {
            SignedJWT signedJWT = parseToken(token);
            JWSVerifier verifier = "access".equals(expectedType) ? accessTokenVerifier : refreshTokenVerifier;

            if (!signedJWT.verify(verifier)) {
                throw new BusinessException(AuthErrorCode.SIGNATURE_FAILED);
            }

            String tokenType = (String) signedJWT.getJWTClaimsSet().getClaim("type");
            if (!expectedType.equals(tokenType)) {
                throw new BusinessException(AuthErrorCode.INVALID_TOKEN_TYPE);
            }

            Object userId = signedJWT.getJWTClaimsSet().getClaim("userId");
            if (userId == null) {
                throw new BusinessException(AuthErrorCode.MISSING_TOKEN_CLAIM);
            }

            return UUID.fromString(userId.toString());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    public MoMoGoUserDetails parseAccessToken(String token) {
        try {
            log.info("[TokenProvider] parseAccessToken 호출됨: 토큰 파싱 시작");
            SignedJWT signedJWT = parseToken(token);
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

            String username = claimsSet.getSubject();
            String userEmail = claimsSet.getStringClaim("userEmail");
            String name = claimsSet.getStringClaim("name");
            String userIdString = claimsSet.getStringClaim("userId");
            UUID userId = userIdString != null ? UUID.fromString(userIdString) : null;

            List<String> roles = claimsSet.getStringListClaim("roles");
            UserRole role = UserRole.USER;

            if (roles != null && !roles.isEmpty()) {
                String roleString = roles.get(0).replace("ROLE_", "");
                role = UserRole.valueOf(roleString);
            }

            UserResponse userResponse = new UserResponse(
                    userId,
                    userEmail,
                    name,
                    null,
                    role,
                    null,
                    false,
                    null
            );

            log.info("[TokenProvider] parseAccessToken 완료: UserResponse 생성");
            return new MoMoGoUserDetails(userResponse, null);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[TokenProvider] parseAccessToken 중 예외 발생: {}", e.getMessage());
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    private SignedJWT parseToken(String token) {
        try {
            return SignedJWT.parse(token);
        } catch (Exception e) {
            throw new BusinessException(AuthErrorCode.MALFORMED_TOKEN);
        }
    }

    private ResponseCookie createCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite(refreshCookieSameSite)
                .build();
    }
}
