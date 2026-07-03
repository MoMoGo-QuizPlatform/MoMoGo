package com.momogo.core.domain.user.exception;

import com.momogo.core.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

  NOT_FOUND(1001, "USER_NOT_FOUND", HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."),
  ALREADY_EXISTS(1002, "USER_ALREADY_EXISTS", HttpStatus.CONFLICT, "이미 존재하는 유저입니다."),
  CURRENT_PASSWORD_REQUIRED(1003, "USER_CURRENT_PASSWORD_REQUIRED", HttpStatus.BAD_REQUEST, "현재 비밀번호가 입력되지 않았습니다."),
  PASSWORD_MISMATCH(1004, "USER_PASSWORD_MISMATCH", HttpStatus.BAD_REQUEST, "현재 비밀번호와 불일치합니다."),
  RESERVED_EMAIL(1005, "USER_RESERVED_EMAIL", HttpStatus.BAD_REQUEST, "사용할 수 없는 예약된 이메일 주소입니다.");

  private final int numeric;
  private final String errorKey;
  private final HttpStatus httpStatus;
  private final String message;

  @Override
  public String getDomain() {
    return "USER";
  }

  @Override
  public String getCode() {
    return getDomain() + "-" + getErrorKey();
  }
}
