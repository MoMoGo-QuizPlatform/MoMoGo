package com.momogo.core.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

  INVALID_TOKEN(6001, "INVALID_TOKEN", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
  EXPIRED_TOKEN(6002, "EXPIRED_TOKEN", HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
  MALFORMED_TOKEN(6003, "MALFORMED_TOKEN", HttpStatus.UNAUTHORIZED, "손상된 토큰 형식입니다."),
  SIGNATURE_FAILED(6004, "SIGNATURE_FAILED", HttpStatus.UNAUTHORIZED, "토큰 서명 검증에 실패했습니다."),
  TOKEN_NOT_FOUND(6005, "TOKEN_NOT_FOUND", HttpStatus.UNAUTHORIZED, "토큰이 존재하지 않습니다."),
  INVALID_TOKEN_TYPE(6006, "INVALID_TOKEN_TYPE", HttpStatus.UNAUTHORIZED, "올바르지 않은 토큰 타입입니다."),
  MISSING_TOKEN_CLAIM(6007, "MISSING_TOKEN_CLAIM", HttpStatus.UNAUTHORIZED, "토큰 내 필수 정보가 누락되었습니다."),

  // 소셜 로그인 관련 에러 코드
  EMAIL_REQUIRED(6008, "EMAIL_REQUIRED", HttpStatus.BAD_REQUEST, "이메일 정보 제공 동의가 필수적입니다."),
  RESTORE_EXPIRED(6009, "RESTORE_EXPIRED", HttpStatus.BAD_REQUEST, "탈퇴 후 30일이 경과하여 복구할 수 없습니다."),
  SOCIAL_TYPE_MISMATCH(6010, "SOCIAL_TYPE_MISMATCH", HttpStatus.BAD_REQUEST, "이미 다른 로그인 방식으로 가입된 이메일 주소입니다. 기존 로그인 방식을 이용해 주세요."),
  SOCIAL_LOGIN_FAILED(6011, "SOCIAL_LOGIN_FAILED", HttpStatus.UNAUTHORIZED, "소셜 로그인 인증에 실패했습니다."),

  LOCK_ACQUISITION_FAILED(6012, "LOCK_ACQUISITION_FAILED", HttpStatus.SERVICE_UNAVAILABLE, "동시 요청 처리를 위한 락 획득에 실패했습니다. 잠시 후 다시 시도해주세요.");

  private final int numeric;
  private final String errorKey;
  private final HttpStatus httpStatus;
  private final String message;

  @Override
  public String getDomain() {
    return "AUTH";
  }

  @Override
  public String getCode() {
    return getDomain() + "-" + getErrorKey();
  }
}
