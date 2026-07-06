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
  MISSING_TOKEN_CLAIM(6007, "MISSING_TOKEN_CLAIM", HttpStatus.UNAUTHORIZED, "토큰 내 필수 정보가 누락되었습니다.");

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
