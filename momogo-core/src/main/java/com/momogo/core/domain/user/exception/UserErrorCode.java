package com.momogo.core.domain.user.exception;

import com.momogo.core.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

  NOT_FOUND(1001, "NOT_FOUND", HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."),
  ALREADY_EXISTS(1002, "ALREADY_EXISTS", HttpStatus.CONFLICT, "이미 존재하는 유저입니다."),
  CURRENT_PASSWORD_REQUIRED(1003, "CURRENT_PASSWORD_REQUIRED", HttpStatus.BAD_REQUEST, "현재 비밀번호가 입력되지 않았습니다."),
  PASSWORD_MISMATCH(1004, "PASSWORD_MISMATCH", HttpStatus.BAD_REQUEST, "현재 비밀번호와 불일치합니다."),
  RESERVED_EMAIL(1005, "RESERVED_EMAIL", HttpStatus.BAD_REQUEST, "사용할 수 없는 예약된 이메일 주소입니다."),
  SOCIAL_USER_CANNOT_CHANGE_PASSWORD(1006, "SOCIAL_USER_CANNOT_CHANGE_PASSWORD", HttpStatus.BAD_REQUEST, "소셜 로그인 계정은 비밀번호를 변경할 수 없습니다."),
  ALREADY_IN_PROGRESS_DELETE(1007, "ALREADY_IN_PROGRESS_DELETE", HttpStatus.BAD_REQUEST, "이미 삭제가 진행 중인 유저입니다."),
  NOT_ABLE_RESTORE(1008, "NOT_ABLE_RESTORE", HttpStatus.BAD_REQUEST, "복구 가능한 계정이 아닙니다.");

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
