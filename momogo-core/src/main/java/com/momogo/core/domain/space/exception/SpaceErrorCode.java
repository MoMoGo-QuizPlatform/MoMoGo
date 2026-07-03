package com.momogo.core.domain.space.exception;

import com.momogo.core.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SpaceErrorCode implements ErrorCode {

  SPACE_USER_NOT_FOUND(2001, "SPACE_USER_NOT_FOUND", HttpStatus.NOT_FOUND, "공간 서비스에 등록되지 않은 사용자입니다."),
  ALREADY_IN_SPACE(2002, "ALREADY_IN_SPACE", HttpStatus.BAD_REQUEST, "이미 소속된 공간이 존재합니다."),
  SPACE_NOT_FOUND(2003, "SPACE_NOT_FOUND", HttpStatus.NOT_FOUND, "존재하지 않는 공간입니다."),
  WRONG_SPACE_PASSWORD(2004, "WRONG_SPACE_PASSWORD", HttpStatus.BAD_REQUEST, "공간 비밀번호가 올바르지 않습니다."),
  NOT_SPACE_ADMIN(2005, "NOT_SPACE_ADMIN", HttpStatus.FORBIDDEN, "해당 공간의 관리자 권한이 없습니다."),
  NOT_SPACE_MEMBER(2006, "NOT_SPACE_MEMBER", HttpStatus.FORBIDDEN, "해당 공간의 소속 멤버가 아닙니다."),
  LAST_ADMIN_DEMOTION_BLOCKED(2007, "LAST_ADMIN_DEMOTION_BLOCKED", HttpStatus.BAD_REQUEST, "공간의 마지막 관리자는 일반 유저로 권한을 변경할 수 없습니다."),
  SELF_ROLE_CHANGE_BLOCKED(2008, "SELF_ROLE_CHANGE_BLOCKED", HttpStatus.BAD_REQUEST, "자신의 권한은 스스로 변경할 수 없습니다.");

  private final int numeric;
  private final String errorKey;
  private final HttpStatus httpStatus;
  private final String message;

  @Override
  public String getDomain() {
    return "SPACE";
  }

  @Override
  public String getCode() {
    return getDomain() + "-" + getErrorKey();
  }
}
