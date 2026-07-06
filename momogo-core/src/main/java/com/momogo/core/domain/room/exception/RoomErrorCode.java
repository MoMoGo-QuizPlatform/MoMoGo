package com.momogo.core.domain.room.exception;

import com.momogo.core.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RoomErrorCode implements ErrorCode {

  ROOM_NOT_FOUND(4001, "ROOM_NOT_FOUND", HttpStatus.NOT_FOUND, "존재하지 않는 평가시험 방입니다."),
  INVALID_TEST_TIME(4002, "INVALID_TEST_TIME", HttpStatus.BAD_REQUEST, "시험 시작 시간은 종료 시간 이전이어야 합니다."),
  ALREADY_ENDED(4003, "ROOM_ALREADY_ENDED", HttpStatus.BAD_REQUEST, "이미 종료된 시험입니다."),
  ROOM_USER_NOT_FOUND(4004, "ROOM_USER_NOT_FOUND", HttpStatus.NOT_FOUND, "응시 대상 유저 중 존재하지 않는 유저가 있습니다."),
  INVALID_ACCESS_BEFORE_START(4005, "INVALID_ACCESS_BEFORE_START", HttpStatus.BAD_REQUEST, "시험 시작 시간 전에는 문제를 조회할 수 없습니다."),
  NOT_ROOM_PARTICIPANT(4006, "NOT_ROOM_PARTICIPANT", HttpStatus.FORBIDDEN, "해당 평가 시험의 응시 대상자가 아닙니다."),
  PROBLEM_NOT_FOUND(4007, "PROBLEM_NOT_FOUND", HttpStatus.NOT_FOUND, "존재하지 않는 문제입니다.");

  private final int numeric;
  private final String errorKey;
  private final HttpStatus httpStatus;
  private final String message;

  @Override
  public String getDomain() {
    return "ROOM";
  }

  @Override
  public String getCode() {
    return getDomain() + "-" + getErrorKey();
  }
}
