package com.momogo.core.domain.room.exception;

import com.momogo.core.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RoomProblemErrorCode implements ErrorCode {

  NOT_FOUND(7001, "NOT_FOUND", HttpStatus.NOT_FOUND, "존재하지 않는 방 문제입니다."),
  ROOM_NOT_STARTED(7002, "ROOM_NOT_STARTED", HttpStatus.FORBIDDEN, "아직 시작되지 않은 시험입니다."),
  ROOM_ACCESS_DENIED(7003, "ROOM_ACCESS_DENIED", HttpStatus.FORBIDDEN, "해당 방에 접근 권한이 없습니다.");

  private final int numeric;
  private final String errorKey;
  private final HttpStatus httpStatus;
  private final String message;

  @Override
  public String getDomain() {
    return "ROOM_PROBLEM";
  }

  @Override
  public String getCode() {
    return getDomain() + "-" + getErrorKey();
  }
}
